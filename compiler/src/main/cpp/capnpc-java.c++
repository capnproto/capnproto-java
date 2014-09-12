// Copyright (c) 2013, Kenton Varda <temporal@gmail.com>
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


// This program is a code generator plugin for `capnp compile` which generates java code.
// It is a modified version of the C++ code generator plugin, capnpc-c++.


#include <capnp/schema.capnp.h>
#include <capnp/serialize.h>
#include <kj/debug.h>
#include <kj/io.h>
#include <kj/string-tree.h>
#include <kj/vector.h>
#include <capnp/schema-loader.h>
#include <capnp/dynamic.h>
#include <unistd.h>
#include <unordered_map>
#include <unordered_set>
#include <set>
#include <kj/main.h>
#include <algorithm>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#if HAVE_CONFIG_H
#include "config.h"
#endif

#ifndef VERSION
#define VERSION "(unknown)"
#endif

namespace capnp {
namespace {

static constexpr uint64_t OUTER_CLASSNAME_ANNOTATION_ID = 0x9b066bb4881f7cd3;
static constexpr uint64_t PACKAGE_ANNOTATION_ID = 0x9ee4c8f803b3b596ull;

static constexpr const char* FIELD_SIZE_NAMES[] = {
  "VOID", "BIT", "BYTE", "TWO_BYTES", "FOUR_BYTES", "EIGHT_BYTES", "POINTER", "INLINE_COMPOSITE"
};

bool hasDiscriminantValue(const schema::Field::Reader& reader) {
  return reader.getDiscriminantValue() != schema::Field::NO_DISCRIMINANT;
}

void enumerateDeps(schema::Type::Reader type, std::set<uint64_t>& deps) {
  switch (type.which()) {
    case schema::Type::STRUCT:
      deps.insert(type.getStruct().getTypeId());
      break;
    case schema::Type::ENUM:
      deps.insert(type.getEnum().getTypeId());
      break;
    case schema::Type::INTERFACE:
      deps.insert(type.getInterface().getTypeId());
      break;
    case schema::Type::LIST:
      enumerateDeps(type.getList().getElementType(), deps);
      break;
    default:
      break;
  }
}

void enumerateDeps(schema::Node::Reader node, std::set<uint64_t>& deps) {
  switch (node.which()) {
    case schema::Node::STRUCT: {
      auto structNode = node.getStruct();
      for (auto field: structNode.getFields()) {
        switch (field.which()) {
          case schema::Field::SLOT:
            enumerateDeps(field.getSlot().getType(), deps);
            break;
          case schema::Field::GROUP:
            deps.insert(field.getGroup().getTypeId());
            break;
        }
      }
      if (structNode.getIsGroup()) {
        deps.insert(node.getScopeId());
      }
      break;
    }
    case schema::Node::INTERFACE: {
      auto interfaceNode = node.getInterface();
      for (auto extend: interfaceNode.getExtends()) {
        deps.insert(extend);
      }
      for (auto method: interfaceNode.getMethods()) {
        deps.insert(method.getParamStructType());
        deps.insert(method.getResultStructType());
      }
      break;
    }
    default:
      break;
  }
}

struct OrderByName {
  template <typename T>
  inline bool operator()(const T& a, const T& b) const {
    return a.getProto().getName() < b.getProto().getName();
  }
};

template <typename MemberList>
kj::Array<uint> makeMembersByName(MemberList&& members) {
  auto sorted = KJ_MAP(member, members) { return member; };
  std::sort(sorted.begin(), sorted.end(), OrderByName());
  return KJ_MAP(member, sorted) { return member.getIndex(); };
}

kj::StringPtr baseName(kj::StringPtr path) {
  KJ_IF_MAYBE(slashPos, path.findLast('/')) {
    return path.slice(*slashPos + 1);
  } else {
    return path;
  }
}

kj::String safeIdentifier(kj::StringPtr identifier) {
  // Given a desired identifier name, munge it to make it safe for use in generated code.
  //
  // If the identifier is a keyword, this adds an underscore to the end.

  static const std::set<kj::StringPtr> keywords({
    "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor", "bool", "break",
    "case", "catch", "char", "char16_t", "char32_t", "class", "compl", "const", "constexpr",
    "const_cast", "continue", "decltype", "default", "delete", "do", "double", "dynamic_cast",
    "else", "enum", "explicit", "export", "extern", "false", "float", "for", "friend", "goto",
    "if", "inline", "int", "long", "mutable", "namespace", "new", "noexcept", "not", "not_eq",
    "nullptr", "operator", "or", "or_eq", "private", "protected", "public", "register",
    "reinterpret_cast", "return", "short", "signed", "sizeof", "static", "static_assert",
    "static_cast", "struct", "switch", "template", "this", "thread_local", "throw", "true",
    "try", "typedef", "typeid", "typename", "union", "unsigned", "using", "virtual", "void",
    "volatile", "wchar_t", "while", "xor", "xor_eq"
  });

  if (keywords.count(identifier) > 0) {
    return kj::str(identifier, '_');
  } else {
    return kj::heapString(identifier);
  }
}

  kj::String spaces(int n) {
    return kj::str(kj::repeat(' ', n * 2));
  }

// =======================================================================================

class CapnpcJavaMain {
public:
  CapnpcJavaMain(kj::ProcessContext& context): context(context) {}

  kj::MainFunc getMain() {
    return kj::MainBuilder(context, "Cap'n Proto Java plugin version " VERSION,
          "This is a Cap'n Proto compiler plugin which generates Java code."
          " This is meant to be run using the Cap'n Proto compiler, e.g.:\n"
          "    capnp compile -ojava foo.capnp")
        .callAfterParsing(KJ_BIND_METHOD(*this, run))
        .build();
  }

private:
  kj::ProcessContext& context;
  SchemaLoader schemaLoader;
  std::unordered_set<uint64_t> usedImports;
  bool hasInterfaces = false;

  kj::StringTree javaFullName(Schema schema) {
    auto node = schema.getProto();
    if (node.getScopeId() == 0) {
      usedImports.insert(node.getId());
      for (auto annotation: node.getAnnotations()) {
        if (annotation.getId() == OUTER_CLASSNAME_ANNOTATION_ID) {
          return kj::strTree("", toTitleCase(annotation.getValue().getText()));
        }
      }
      return kj::strTree(" ");//kj::strTree(outerClassName);
    } else {
      Schema parent = schemaLoader.get(node.getScopeId());
      for (auto nested: parent.getProto().getNestedNodes()) {
        if (nested.getId() == node.getId()) {
          return kj::strTree(javaFullName(parent), ".", nested.getName());
        }
      }
      KJ_FAIL_REQUIRE("A schema Node's supposed scope did not contain the node as a NestedNode.");
    }
  }


  kj::String toUpperCase(kj::StringPtr name) {
    kj::Vector<char> result(name.size() + 4);

    for (char c: name) {
      if ('a' <= c && c <= 'z') {
        result.add(c - 'a' + 'A');
      } else if (result.size() > 0 && 'A' <= c && c <= 'Z') {
        result.add('_');
        result.add(c);
      } else {
        result.add(c);
      }
    }

    result.add('\0');

    return kj::String(result.releaseAsArray());
  }

  kj::String toTitleCase(kj::StringPtr name) {
    kj::String result = kj::heapString(name);
    if ('a' <= result[0] && result[0] <= 'z') {
      result[0] = result[0] - 'a' + 'A';
    }
    return kj::mv(result);
  }

  kj::StringTree typeName(schema::Type::Reader type) {
    switch (type.which()) {
      case schema::Type::VOID: return kj::strTree("org.capnproto.Void");

      case schema::Type::BOOL: return kj::strTree("boolean");
      case schema::Type::INT8: return kj::strTree("byte");
      case schema::Type::INT16: return kj::strTree("short");
      case schema::Type::INT32: return kj::strTree("int");
      case schema::Type::INT64: return kj::strTree("long");
      case schema::Type::UINT8: return kj::strTree("byte");
      case schema::Type::UINT16: return kj::strTree("short");
      case schema::Type::UINT32: return kj::strTree("int");
      case schema::Type::UINT64: return kj::strTree("long");
      case schema::Type::FLOAT32: return kj::strTree("float");
      case schema::Type::FLOAT64: return kj::strTree("double");

      case schema::Type::TEXT: return kj::strTree(" org.capnproto.Text");
      case schema::Type::DATA: return kj::strTree(" org.capnproto.Data");

      case schema::Type::ENUM:
        return javaFullName(schemaLoader.get(type.getEnum().getTypeId()));
      case schema::Type::STRUCT:
        return javaFullName(schemaLoader.get(type.getStruct().getTypeId()));
      case schema::Type::INTERFACE:
        return javaFullName(schemaLoader.get(type.getInterface().getTypeId()));

      case schema::Type::LIST:
      {
        auto elementType = type.getList().getElementType();
        switch (elementType.which()) {
        case schema::Type::VOID:
          return kj::strTree(" org.capnproto.PrimitiveList.Void");
        case schema::Type::BOOL:
          return kj::strTree(" org.capnproto.PrimitiveList.Boolean");
        case schema::Type::INT8:
        case schema::Type::UINT8:
          return kj::strTree(" org.capnproto.PrimitiveList.Byte");
        case schema::Type::INT16:
        case schema::Type::UINT16:
          return kj::strTree(" org.capnproto.PrimitiveList.Short");
        case schema::Type::INT32:
        case schema::Type::UINT32:
          return kj::strTree(" org.capnproto.PrimitiveList.Int");
        case schema::Type::INT64:
        case schema::Type::UINT64:
          return kj::strTree(" org.capnproto.PrimitiveList.Long");
        case schema::Type::FLOAT32:
          return kj::strTree(" org.capnproto.PrimitiveList.Float");
        case schema::Type::FLOAT64:
          return kj::strTree(" org.capnproto.PrimitiveList.Double");
        case schema::Type::STRUCT:
          return kj::strTree(" org.capnproto.StructList");
        case schema::Type::TEXT:
          return kj::strTree( "org.capnproto.TextList");
        case schema::Type::DATA:
          return kj::strTree( "org.capnproto.DataList");
        case schema::Type::ENUM:
        case schema::Type::INTERFACE:
        case schema::Type::ANY_POINTER:
        case schema::Type::LIST:
          KJ_FAIL_REQUIRE("unimplemented");
        }
        KJ_UNREACHABLE;
      }
      case schema::Type::ANY_POINTER:
        // Not used.
        return kj::strTree();
    }
    KJ_UNREACHABLE;
  }

  kj::StringTree literalValue(schema::Type::Reader type, schema::Value::Reader value) {
    switch (value.which()) {
      case schema::Value::VOID: return kj::strTree("org.capnproto.Void.VOID");
      case schema::Value::BOOL: return kj::strTree(value.getBool() ? "true" : "false");
      case schema::Value::INT8: return kj::strTree(value.getInt8());
      case schema::Value::INT16: return kj::strTree(value.getInt16());
      case schema::Value::INT32: return kj::strTree(value.getInt32());
      case schema::Value::INT64: return kj::strTree(value.getInt64(), "L");
      case schema::Value::UINT8: return kj::strTree(kj::implicitCast<int8_t>(value.getUint8()));
      case schema::Value::UINT16: return kj::strTree(kj::implicitCast<int16_t>(value.getUint16()));
      case schema::Value::UINT32: return kj::strTree(kj::implicitCast<int32_t>(value.getUint32()));
      case schema::Value::UINT64: return kj::strTree(kj::implicitCast<int64_t>(value.getUint64()), "L");
      case schema::Value::FLOAT32: return kj::strTree(value.getFloat32(), "f");
      case schema::Value::FLOAT64: return kj::strTree(value.getFloat64());
      case schema::Value::ENUM: {
        EnumSchema schema = schemaLoader.get(type.getEnum().getTypeId()).asEnum();
        if (value.getEnum() < schema.getEnumerants().size()) {
          return kj::strTree(
              javaFullName(schema), "::",
              toUpperCase(schema.getEnumerants()[value.getEnum()].getProto().getName()));
        } else {
          return kj::strTree("static_cast<", javaFullName(schema), ">(", value.getEnum(), ")");
        }
      }

      case schema::Value::TEXT:
      case schema::Value::DATA:
      case schema::Value::STRUCT:
      case schema::Value::INTERFACE:
      case schema::Value::LIST:
      case schema::Value::ANY_POINTER:
        KJ_FAIL_REQUIRE("literalValue() can only be used on primitive types.");
    }
    KJ_UNREACHABLE;
  }

  // -----------------------------------------------------------------
  // Code to deal with "slots" -- determines what to zero out when we clear a group.

  static uint typeSizeBits(schema::Type::Which whichType) {
    switch (whichType) {
      case schema::Type::BOOL: return 1;
      case schema::Type::INT8: return 8;
      case schema::Type::INT16: return 16;
      case schema::Type::INT32: return 32;
      case schema::Type::INT64: return 64;
      case schema::Type::UINT8: return 8;
      case schema::Type::UINT16: return 16;
      case schema::Type::UINT32: return 32;
      case schema::Type::UINT64: return 64;
      case schema::Type::FLOAT32: return 32;
      case schema::Type::FLOAT64: return 64;
      case schema::Type::ENUM: return 16;

      case schema::Type::VOID:
      case schema::Type::TEXT:
      case schema::Type::DATA:
      case schema::Type::LIST:
      case schema::Type::STRUCT:
      case schema::Type::INTERFACE:
      case schema::Type::ANY_POINTER:
        KJ_FAIL_REQUIRE("Should only be called for data types.");
    }
    KJ_UNREACHABLE;
  }

  enum class Section {
    NONE,
    DATA,
    POINTERS
  };

  static Section sectionFor(schema::Type::Which whichType) {
    switch (whichType) {
      case schema::Type::VOID:
        return Section::NONE;
      case schema::Type::BOOL:
      case schema::Type::INT8:
      case schema::Type::INT16:
      case schema::Type::INT32:
      case schema::Type::INT64:
      case schema::Type::UINT8:
      case schema::Type::UINT16:
      case schema::Type::UINT32:
      case schema::Type::UINT64:
      case schema::Type::FLOAT32:
      case schema::Type::FLOAT64:
      case schema::Type::ENUM:
        return Section::DATA;
      case schema::Type::TEXT:
      case schema::Type::DATA:
      case schema::Type::LIST:
      case schema::Type::STRUCT:
      case schema::Type::INTERFACE:
      case schema::Type::ANY_POINTER:
        return Section::POINTERS;
    }
    KJ_UNREACHABLE;
  }

  static kj::StringPtr maskType(schema::Type::Which whichType) {
    switch (whichType) {
      case schema::Type::BOOL: return "bool";
      case schema::Type::INT8: return " ::uint8_t";
      case schema::Type::INT16: return " ::uint16_t";
      case schema::Type::INT32: return " ::uint32_t";
      case schema::Type::INT64: return " ::uint64_t";
      case schema::Type::UINT8: return " ::uint8_t";
      case schema::Type::UINT16: return " ::uint16_t";
      case schema::Type::UINT32: return " ::uint32_t";
      case schema::Type::UINT64: return " ::uint64_t";
      case schema::Type::FLOAT32: return " ::uint32_t";
      case schema::Type::FLOAT64: return " ::uint64_t";
      case schema::Type::ENUM: return " ::uint16_t";

      case schema::Type::VOID:
      case schema::Type::TEXT:
      case schema::Type::DATA:
      case schema::Type::LIST:
      case schema::Type::STRUCT:
      case schema::Type::INTERFACE:
      case schema::Type::ANY_POINTER:
        KJ_FAIL_REQUIRE("Should only be called for data types.");
    }
    KJ_UNREACHABLE;
  }

  struct Slot {
    schema::Type::Which whichType;
    uint offset;

    bool isSupersetOf(Slot other) const {
      auto section = sectionFor(whichType);
      if (section != sectionFor(other.whichType)) return false;
      switch (section) {
        case Section::NONE:
          return true;  // all voids overlap
        case Section::DATA: {
          auto bits = typeSizeBits(whichType);
          auto start = offset * bits;
          auto otherBits = typeSizeBits(other.whichType);
          auto otherStart = other.offset * otherBits;
          return start <= otherStart && otherStart + otherBits <= start + bits;
        }
        case Section::POINTERS:
          return offset == other.offset;
      }
      KJ_UNREACHABLE;
    }

    bool operator<(Slot other) const {
      // Sort by section, then start position, and finally size.

      auto section = sectionFor(whichType);
      auto otherSection = sectionFor(other.whichType);
      if (section < otherSection) {
        return true;
      } else if (section > otherSection) {
        return false;
      }

      switch (section) {
        case Section::NONE:
          return false;
        case Section::DATA: {
          auto bits = typeSizeBits(whichType);
          auto start = offset * bits;
          auto otherBits = typeSizeBits(other.whichType);
          auto otherStart = other.offset * otherBits;
          if (start < otherStart) {
            return true;
          } else if (start > otherStart) {
            return false;
          }

          // Sort larger sizes before smaller.
          return bits > otherBits;
        }
        case Section::POINTERS:
          return offset < other.offset;
      }
      KJ_UNREACHABLE;
    }
  };

  void getSlots(StructSchema schema, kj::Vector<Slot>& slots) {
    auto structProto = schema.getProto().getStruct();
    if (structProto.getDiscriminantCount() > 0) {
      slots.add(Slot { schema::Type::UINT16, structProto.getDiscriminantOffset() });
    }

    for (auto field: schema.getFields()) {
      auto proto = field.getProto();
      switch (proto.which()) {
        case schema::Field::SLOT: {
          auto slot = proto.getSlot();
          slots.add(Slot { slot.getType().which(), slot.getOffset() });
          break;
        }
        case schema::Field::GROUP:
          getSlots(schema.getDependency(proto.getGroup().getTypeId()).asStruct(), slots);
          break;
      }
    }
  }

  kj::Array<Slot> getSortedSlots(StructSchema schema) {
    // Get a representation of all of the field locations owned by this schema, e.g. so that they
    // can be zero'd out.

    kj::Vector<Slot> slots(schema.getFields().size());
    getSlots(schema, slots);
    std::sort(slots.begin(), slots.end());

    kj::Vector<Slot> result(slots.size());

    // All void slots are redundant, and they sort towards the front of the list.  By starting out
    // with `prevSlot` = void, we will end up skipping them all, which is what we want.
    Slot prevSlot = { schema::Type::VOID, 0 };
    for (auto slot: slots) {
      if (prevSlot.isSupersetOf(slot)) {
        // This slot is redundant as prevSlot is a superset of it.
        continue;
      }

      // Since all sizes are power-of-two, if two slots overlap at all, one must be a superset of
      // the other.  Since we sort slots by starting position, we know that the only way `slot`
      // could be a superset of `prevSlot` is if they have the same starting position.  However,
      // since we sort slots with the same starting position by descending size, this is not
      // possible.
      KJ_DASSERT(!slot.isSupersetOf(prevSlot));

      result.add(slot);

      prevSlot = slot;
    }

    return result.releaseAsArray();
  }

  // -----------------------------------------------------------------

  struct DiscriminantChecks {
    kj::String has;
    kj::String check;
    kj::String set;
    kj::StringTree readerIsDecl;
    kj::StringTree builderIsDecl;
    kj::StringTree isDefs;
  };

  DiscriminantChecks makeDiscriminantChecks(kj::StringPtr scope,
                                            kj::StringPtr memberName,
                                            StructSchema containingStruct,
                                            int indent) {
    auto discrimOffset = containingStruct.getProto().getStruct().getDiscriminantOffset();

    kj::String titleCase = toTitleCase(memberName);
    kj::String upperCase = toUpperCase(memberName);

    return DiscriminantChecks {
        kj::str(
            "  if (which() != ", scope, upperCase, ") return false;\n"),
        kj::str(
          "  KJ_IREQUIRE(which() == ", scope, upperCase, ",\n"
          "              \"Must check which() before get()ing a union member.\");\n"),
        kj::str(
          spaces(indent), "  _builder.setShortField(", discrimOffset, ", (short)",
          scope, "Which.", upperCase, ".ordinal());\n"),
          kj::strTree(spaces(indent), "  public final boolean is", titleCase, "() {\n",
                      spaces(indent), "    return which() == ", scope, "Which.", upperCase,";\n",
                      spaces(indent), "  }\n"),
          kj::strTree(spaces(indent), "public final boolean is", titleCase, "() {\n",
                      spaces(indent), "  return which() == ", scope, "Which.", upperCase, ";\n",
                      spaces(indent), "}\n"),
          kj::strTree(
            "inline boolean ", scope, "Reader::is", titleCase, "() const {\n"
            "  return which() == ", scope, upperCase, ";\n"
            "}\n"
            "inline boolean ", scope, "Builder::is", titleCase, "() {\n"
            "  return which() == ", scope, upperCase, ";\n"
            "}\n")
    };
  }

  // -----------------------------------------------------------------

  struct FieldText {
    kj::StringTree readerMethodDecls;
    kj::StringTree builderMethodDecls;
    kj::StringTree pipelineMethodDecls;
    kj::StringTree inlineMethodDefs;
  };

  enum class FieldKind {
    PRIMITIVE,
    BLOB,
    STRUCT,
    LIST,
    INTERFACE,
    ANY_POINTER
  };

  FieldText makeFieldText(kj::StringPtr scope, StructSchema::Field field, int indent) {
    auto proto = field.getProto();
    kj::String titleCase = toTitleCase(proto.getName());

    DiscriminantChecks unionDiscrim;
    if (hasDiscriminantValue(proto)) {
      unionDiscrim = makeDiscriminantChecks(scope, proto.getName(), field.getContainingStruct(), indent + 1);
    }

    switch (proto.which()) {
      case schema::Field::SLOT:
        // Continue below.
        break;

      case schema::Field::GROUP: {
        auto slots = getSortedSlots(schemaLoader.get(
            field.getProto().getGroup().getTypeId()).asStruct());
        return FieldText {
          kj::strTree(
            kj::mv(unionDiscrim.readerIsDecl),
            spaces(indent), "  public ", titleCase, ".Reader get", titleCase, "() {\n",
            spaces(indent), "    return new ", scope, titleCase, ".Reader(_reader);\n",
            spaces(indent), "  }\n",
            "\n"),

            kj::strTree(
              kj::mv(unionDiscrim.builderIsDecl),
              spaces(indent), "  public final ", titleCase, ".Builder get", titleCase, "() {\n",
              spaces(indent), "    return new ", scope, titleCase, ".Builder(_builder);\n",
              spaces(indent), "  }\n",
              spaces(indent), "  public final ", titleCase, ".Builder init", titleCase, "() {\n",
              spaces(indent), "    throw new Error();\n",
              spaces(indent), "  }\n",
              "\n"),

            kj::strTree(),

            kj::strTree(
                kj::mv(unionDiscrim.isDefs),
                "inline ", scope, titleCase, "::Reader ", scope, "Reader::get", titleCase, "() const {\n",
                unionDiscrim.check,
                "  return ", scope, titleCase, "::Reader(_reader);\n"
                "}\n"
                "inline ", scope, titleCase, "::Builder ", scope, "Builder::get", titleCase, "() {\n",
                unionDiscrim.check,
                "  return ", scope, titleCase, "::Builder(_builder);\n"
                "}\n",
                hasDiscriminantValue(proto) ? kj::strTree() : kj::strTree(
                  "inline ", scope, titleCase, "::Pipeline ", scope, "Pipeline::get", titleCase, "() {\n",
                  "  return ", scope, titleCase, "::Pipeline(_typeless.noop());\n"
                  "}\n"),
                "inline ", scope, titleCase, "::Builder ", scope, "Builder::init", titleCase, "() {\n",
                unionDiscrim.set,
                KJ_MAP(slot, slots) {
                  switch (sectionFor(slot.whichType)) {
                    case Section::NONE:
                      return kj::strTree();
                    case Section::DATA:
                      return kj::strTree(
                          "  _builder.setDataField<", maskType(slot.whichType), ">(",
                              slot.offset, " * ::capnp::ELEMENTS, 0);\n");
                    case Section::POINTERS:
                      return kj::strTree(
                          "  _builder.getPointerField(", slot.offset,
                              " * ::capnp::POINTERS).clear();\n");
                  }
                  KJ_UNREACHABLE;
                },
                "  return ", scope, titleCase, "::Builder(_builder);\n"
                "}\n")
          };
      }
    }

    auto slot = proto.getSlot();

    FieldKind kind = FieldKind::PRIMITIVE;
    kj::String ownedType;
    kj::String type = typeName(slot.getType()).flatten();
    kj::StringPtr setterDefault;  // only for void
    kj::String defaultMask;    // primitives only
    size_t defaultOffset = 0;    // pointers only: offset of the default value within the schema.
    size_t defaultSize = 0;      // blobs only: byte size of the default value.

    auto typeBody = slot.getType();
    auto defaultBody = slot.getDefaultValue();
    switch (typeBody.which()) {
      case schema::Type::VOID:
        kind = FieldKind::PRIMITIVE;
        setterDefault = " = ::capnp::VOID";
        break;

#define HANDLE_PRIMITIVE(discrim, typeName, javaTypeName, defaultName, suffix) \
      case schema::Type::discrim: \
        kind = FieldKind::PRIMITIVE; \
        if (defaultBody.get##defaultName() != 0) { \
          defaultMask = kj::str("(", #javaTypeName, ")", kj::implicitCast< typeName>(defaultBody.get##defaultName()), #suffix); \
        } \
        break;

        HANDLE_PRIMITIVE(BOOL, bool, boolean, Bool, );
        HANDLE_PRIMITIVE(INT8 , ::int8_t , byte, Int8 , );
        HANDLE_PRIMITIVE(INT16, ::int16_t, short, Int16, );
        HANDLE_PRIMITIVE(INT32, ::int32_t, int, Int32, );
        HANDLE_PRIMITIVE(INT64, ::int64_t, long, Int64, L);
        HANDLE_PRIMITIVE(UINT8 , ::int8_t , byte, Uint8 , );
        HANDLE_PRIMITIVE(UINT16, ::int16_t, short, Uint16, );
        HANDLE_PRIMITIVE(UINT32, ::int32_t, int, Uint32, );
        HANDLE_PRIMITIVE(UINT64, ::int64_t, long, Uint64, L);
#undef HANDLE_PRIMITIVE

      case schema::Type::FLOAT32:
        kind = FieldKind::PRIMITIVE;
        if (defaultBody.getFloat32() != 0) {
          int32_t mask;
          float value = defaultBody.getFloat32();
          static_assert(sizeof(mask) == sizeof(value), "bug");
          memcpy(&mask, &value, sizeof(mask));
          defaultMask = kj::str(mask);
        }
        break;

      case schema::Type::FLOAT64:
        kind = FieldKind::PRIMITIVE;
        if (defaultBody.getFloat64() != 0) {
          int64_t mask;
          double value = defaultBody.getFloat64();
          static_assert(sizeof(mask) == sizeof(value), "bug");
          memcpy(&mask, &value, sizeof(mask));
          defaultMask = kj::str(mask, "L");
        }
        break;

      case schema::Type::TEXT:
        kind = FieldKind::BLOB;
        if (defaultBody.hasText()) {
          defaultOffset = field.getDefaultValueSchemaOffset();
          defaultSize = defaultBody.getText().size();
        }
        break;
      case schema::Type::DATA:
        kind = FieldKind::BLOB;
        if (defaultBody.hasData()) {
          defaultOffset = field.getDefaultValueSchemaOffset();
          defaultSize = defaultBody.getData().size();
        }
        break;

      case schema::Type::ENUM:
        kind = FieldKind::PRIMITIVE;
        if (defaultBody.getEnum() != 0) {
          defaultMask = kj::str(defaultBody.getEnum(), "u");
        }
        break;

      case schema::Type::STRUCT:
        kind = FieldKind::STRUCT;
        if (defaultBody.hasStruct()) {
          defaultOffset = field.getDefaultValueSchemaOffset();
        }
        break;
      case schema::Type::LIST:
        kind = FieldKind::LIST;
        if (defaultBody.hasList()) {
          defaultOffset = field.getDefaultValueSchemaOffset();
        }
        break;
      case schema::Type::INTERFACE:
        kind = FieldKind::INTERFACE;
        break;
      case schema::Type::ANY_POINTER:
        kind = FieldKind::ANY_POINTER;
        if (defaultBody.hasAnyPointer()) {
          defaultOffset = field.getDefaultValueSchemaOffset();
        }
        break;
    }

    kj::String defaultMaskParam;
    if (defaultMask.size() > 0) {
      defaultMaskParam = kj::str(", ", defaultMask);
    }

    uint offset = slot.getOffset();

    if (kind == FieldKind::PRIMITIVE) {
      return FieldText {
        kj::strTree(
            kj::mv(unionDiscrim.readerIsDecl),
            spaces(indent), "  public final ", type, " get", titleCase, "() {\n",
            spaces(indent),
            (typeBody.which() == schema::Type::ENUM ?
             kj::strTree("    return ", type, ".values()[_reader.getShortField(", offset, ")];\n") :
             (typeBody.which() == schema::Type::VOID ?
              kj::strTree("    return org.capnproto.Void.VOID;\n") :
              kj::strTree("    return _reader.get",toTitleCase(type),"Field(", offset, defaultMaskParam, ");\n"))),
            spaces(indent), "  }\n",
            "\n"),

          kj::strTree(
            kj::mv(unionDiscrim.builderIsDecl),
            spaces(indent), "  public final ", type, " get", titleCase, "() {\n",
            spaces(indent),
            (typeBody.which() == schema::Type::ENUM ?
             kj::strTree("    return ", type, ".values()[_builder.getShortField(", offset, ")];\n") :
             (typeBody.which() == schema::Type::VOID ?
              kj::strTree("    return org.capnproto.Void.VOID;\n") :
              kj::strTree("    return _builder.get",toTitleCase(type),"Field(", offset, defaultMaskParam, ");\n"))),
            spaces(indent), "  }\n",

            spaces(indent), "  public final void set", titleCase, "(", type, " value) {\n",
            unionDiscrim.set,
            (typeBody.which() == schema::Type::ENUM ?
             kj::strTree(spaces(indent), "    _builder.setShortField(", offset, ", (short)value.ordinal());\n") :
             (typeBody.which() == schema::Type::VOID ?
              kj::strTree() :
              kj::strTree(spaces(indent), "    _builder.set",
                          toTitleCase(type), "Field(", offset, ", value", defaultMaskParam, ");\n"))),
            spaces(indent), "  }\n",
            "\n"),

        kj::strTree(),

        kj::strTree(
            kj::mv(unionDiscrim.isDefs),
            "inline ", type, " ", scope, "Reader::get", titleCase, "() const {\n",
            unionDiscrim.check,
            "  return _reader.getDataField<", type, ">(\n"
            "      ", offset, " * ::capnp::ELEMENTS", defaultMaskParam, ");\n",
            "}\n"
            "\n"
            "inline ", type, " ", scope, "Builder::get", titleCase, "() {\n",
            unionDiscrim.check,
            "  return _builder.getDataField<", type, ">(\n"
            "      ", offset, " * ::capnp::ELEMENTS", defaultMaskParam, ");\n",
            "}\n"
            "inline void ", scope, "Builder::set", titleCase, "(", type, " value) {\n",
            unionDiscrim.set,
            "  _builder.setDataField<", type, ">(\n"
            "      ", offset, " * ::capnp::ELEMENTS, value", defaultMaskParam, ");\n",
            "}\n"
            "\n")
      };

    } else if (kind == FieldKind::INTERFACE) {
      KJ_FAIL_REQUIRE("interfaces unimplemented");
    } else if (kind == FieldKind::ANY_POINTER) {
      return FieldText {
        kj::strTree(
            kj::mv(unionDiscrim.readerIsDecl),
            "  inline boolean has", titleCase, "() const;\n"
            "  inline ::capnp::AnyPointer::Reader get", titleCase, "() const;\n"
            "\n"),

        kj::strTree(
            kj::mv(unionDiscrim.builderIsDecl),
            "  inline boolean has", titleCase, "();\n"
            "  inline ::capnp::AnyPointer::Builder get", titleCase, "();\n"
            "  inline ::capnp::AnyPointer::Builder init", titleCase, "();\n"
            "\n"),

        kj::strTree(),

        kj::strTree(
            kj::mv(unionDiscrim.isDefs),
            "inline boolean ", scope, "Reader::has", titleCase, "() const {\n",
            unionDiscrim.has,
            "  return !_reader.getPointerField(", offset, " * ::capnp::POINTERS).isNull();\n"
            "}\n"
            "inline boolean ", scope, "Builder::has", titleCase, "() {\n",
            unionDiscrim.has,
            "  return !_builder.getPointerField(", offset, " * ::capnp::POINTERS).isNull();\n"
            "}\n"
            "inline ::capnp::AnyPointer::Reader ", scope, "Reader::get", titleCase, "() const {\n",
            unionDiscrim.check,
            "  return ::capnp::AnyPointer::Reader(\n"
            "      _reader.getPointerField(", offset, " * ::capnp::POINTERS));\n"
            "}\n"
            "inline ::capnp::AnyPointer::Builder ", scope, "Builder::get", titleCase, "() {\n",
            unionDiscrim.check,
            "  return ::capnp::AnyPointer::Builder(\n"
            "      _builder.getPointerField(", offset, " * ::capnp::POINTERS));\n"
            "}\n"
            "inline ::capnp::AnyPointer::Builder ", scope, "Builder::init", titleCase, "() {\n",
            unionDiscrim.set,
            "  auto result = ::capnp::AnyPointer::Builder(\n"
            "      _builder.getPointerField(", offset, " * ::capnp::POINTERS));\n"
            "  result.clear();\n"
            "  return result;\n"
            "}\n"
            "\n")
      };

    } else if (kind == FieldKind::STRUCT) {

      return FieldText {
        kj::strTree(
          kj::mv(unionDiscrim.readerIsDecl),
          spaces(indent), "  public boolean has", titleCase, "() {\n",
          spaces(indent), "    return !_reader.getPointerField(", offset, ").isNull();\n",
          spaces(indent), "  }\n",

          spaces(indent), "  public ", type, ".Reader",
          " get", titleCase, "() {\n",
          spaces(indent), "    return ", type,
          ".factory.fromStructReader(_reader.getPointerField(", offset,").getStruct());\n",
          spaces(indent), "  }\n", "\n"),

        kj::strTree(
          kj::mv(unionDiscrim.builderIsDecl),
          spaces(indent), "  public final boolean has", titleCase, "() {\n",
          spaces(indent), "    return !_builder.getPointerField(", offset, ").isNull();\n",
          spaces(indent), "  }\n",
          spaces(indent), "  public final ", type, ".Builder get", titleCase, "() {\n",
          spaces(indent), "    return ", type,
          ".factory.fromStructBuilder(_builder.getPointerField(", offset, ").getStruct(",
          type, ".STRUCT_SIZE", "));\n",
          spaces(indent), "  }\n",
          spaces(indent), "  public final void set", titleCase, "(", type, ".Reader value) {\n",
          unionDiscrim.set,
          spaces(indent), "    throw new Error();\n",
          spaces(indent), "  }\n",
          spaces(indent), "  public final ", type, ".Builder init", titleCase, "() {\n",
          unionDiscrim.set,
          spaces(indent), "    return ",
          type, ".factory.fromStructBuilder(_builder.getPointerField(", offset, ").initStruct(",
          type, ".STRUCT_SIZE", "));\n",
          spaces(indent), "  }\n"),

        kj::strTree(),
        kj::strTree()
      };

    } else if (kind == FieldKind::BLOB) {

      kj::String blobKind = typeBody.which() == schema::Type::TEXT ? kj::str("Text") : kj::str("Data");

      return FieldText {
        kj::strTree(
          kj::mv(unionDiscrim.readerIsDecl),
          spaces(indent), "  public boolean has", titleCase, "() {\n",
          spaces(indent), "    return !_reader.getPointerField(", offset, ").isNull();\n",
          spaces(indent), "  }\n",

          spaces(indent), "  public ", type, ".Reader",
          " get", titleCase, "() {\n",
          spaces(indent), "    return _reader.getPointerField(",
          offset, ").get", blobKind, " ();\n", // XXX
          spaces(indent), "  }\n", "\n"),

        kj::strTree(
          kj::mv(unionDiscrim.builderIsDecl),
          spaces(indent), "  public final boolean has", titleCase, "() {\n",
          spaces(indent), "    return !_builder.getPointerField(", offset, ").isNull();\n",
          spaces(indent), "  }\n",
          spaces(indent), "  public final ", type, ".Builder get", titleCase, "() {\n",
          spaces(indent), "    return _builder.getPointerField(",
          offset, ").get", blobKind, " ();\n", // XXX
          spaces(indent), "  }\n",
          spaces(indent), "  public final void set", titleCase, "(", type, ".Reader value) {\n",
          unionDiscrim.set,
          spaces(indent), "    _builder.getPointerField(", offset, ").set", blobKind, "(value);\n",
          spaces(indent), "  }\n",
          spaces(indent), "  public final ", type, ".Builder init", titleCase, "(int size) {\n",
          spaces(indent), "    throw new Error();\n",
          spaces(indent), "  }\n"),

        kj::strTree(),
        kj::strTree()
      };
    } else if (kind == FieldKind::LIST) {

      uint64_t typeId = field.getContainingStruct().getProto().getId();
      kj::String defaultParam = defaultOffset == 0 ? kj::str() : kj::str(
          ",\n        ::capnp::schemas::s_", kj::hex(typeId), ".encodedNode + ", defaultOffset,
          defaultSize == 0 ? kj::strTree() : kj::strTree(", ", defaultSize));

      kj::String elementReaderType;
      kj::String elementBuilderType;
      kj::String builderFactoryArg = kj::str("");
      kj::String readerFactoryArg = kj::str("");
      kj::String fieldSize;
      kj::String readerClass = kj::str("Reader");
      kj::String builderClass = kj::str("Builder");
      bool isStructOrCapList = false;
      bool isStructList = false;
      if (kind == FieldKind::LIST) {
        bool primitiveElement = false;
        bool interface = false;
        switch (typeBody.getList().getElementType().which()) {
          case schema::Type::VOID:
            primitiveElement = true;
            fieldSize = kj::str("org.capnproto.FieldSize.VOID");
            break;
          case schema::Type::BOOL:
            primitiveElement = true;
            fieldSize = kj::str("org.capnproto.FieldSize.BIT");
            break;

          case schema::Type::INT8:
          case schema::Type::UINT8:
            primitiveElement = true;
            fieldSize = kj::str("org.capnproto.FieldSize.BYTE");
            break;


          case schema::Type::INT16:
          case schema::Type::UINT16:
            primitiveElement = true;
            fieldSize = kj::str("org.capnproto.FieldSize.TWO_BYTES");
            break;

          case schema::Type::INT32:
          case schema::Type::UINT32:
          case schema::Type::FLOAT32:
            primitiveElement = true;
            fieldSize = kj::str("org.capnproto.FieldSize.FOUR_BYTES");
            break;

          case schema::Type::INT64:
          case schema::Type::UINT64:
          case schema::Type::FLOAT64:
            primitiveElement = true;
            fieldSize = kj::str("org.capnproto.FieldSize.EIGHT_BYTES");
            break;

          case schema::Type::ENUM:
            primitiveElement = true;
            fieldSize = kj::str("org.capnproto.FieldSize.TWO_BYTES");
            break;

          case schema::Type::TEXT:
            primitiveElement = false;
            fieldSize = kj::str("org.capnproto.FieldSize.POINTER");
            break;
          case schema::Type::DATA:
            primitiveElement = false;
            fieldSize = kj::str("org.capnproto.FieldSize.POINTER");
            break;
          case schema::Type::LIST:
          case schema::Type::ANY_POINTER:
            primitiveElement = false;
            break;

          case schema::Type::INTERFACE:
            isStructOrCapList = true;
            primitiveElement = false;
            interface = true;
            break;

          case schema::Type::STRUCT:
            isStructList = true;
            isStructOrCapList = true;
            primitiveElement = false;
            elementReaderType = kj::str(typeName(typeBody.getList().getElementType()), ".Reader");
            readerClass = kj::str("Reader<", elementReaderType, ">");
            elementBuilderType = kj::str(typeName(typeBody.getList().getElementType()), ".Builder");
            builderClass = kj::str("Builder<", elementBuilderType, ">");
            readerFactoryArg = kj::str(typeName(typeBody.getList().getElementType()), ".factory, ");
            builderFactoryArg = kj::str(typeName(typeBody.getList().getElementType()), ".factory, ");
            fieldSize = kj::str(typeName(typeBody.getList().getElementType()),".STRUCT_SIZE.preferredListEncoding");
            break;
        }
        if (primitiveElement) {
          elementReaderType = kj::str(typeName(typeBody.getList().getElementType()));
          elementBuilderType = kj::str(typeName(typeBody.getList().getElementType()));
        }
      }


      return FieldText {
        kj::strTree(
            kj::mv(unionDiscrim.readerIsDecl),
            spaces(indent), "  public final boolean has", titleCase, "() {\n",
            spaces(indent), "    return !_reader.getPointerField(", offset, ").isNull();\n",
            spaces(indent), "  }\n",

            spaces(indent), "  public final ", type, ".", readerClass,
            " get", titleCase, "() {\n",
            spaces(indent), "    return new ", type, ".", readerClass, "(\n",
            spaces(indent), "      ", readerFactoryArg, "_reader.getPointerField(", offset, ").getList(",
            fieldSize, ")",
            ");\n",
            spaces(indent), "  }\n",
            "\n"),

        kj::strTree(
            kj::mv(unionDiscrim.builderIsDecl),
            spaces(indent), "  public final boolean has", titleCase, "() {\n",
            spaces(indent), "    return !_builder.getPointerField(", offset, ").isNull();\n",
            spaces(indent), "  }\n",

            spaces(indent), "  public final ", type, ".", builderClass,
            " get", titleCase, "() {\n",
            spaces(indent), "    return new ", type, ".", builderClass, " (\n",
            spaces(indent), "      ", builderFactoryArg, "_builder.getPointerField(", offset, ").get",
            (isStructList ?
             kj::strTree("StructList(", typeName(typeBody.getList().getElementType()),".STRUCT_SIZE)") :
             kj::strTree("List(", fieldSize, ")")),
            ");\n",
            spaces(indent), "  }\n",

            spaces(indent), "  public final void set", titleCase, "(", type, ".Reader value) {\n",
            spaces(indent), "    throw new Error();\n",
            spaces(indent), "  }\n",

            spaces(indent), "  public final ", type, ".", builderClass,
            " init", titleCase, "(int size) {\n",
            spaces(indent), "    return new ", type, ".", builderClass, "(\n",
            spaces(indent), "      ", builderFactoryArg, "_builder.getPointerField(", offset, ").init",
            (isStructList ?
             kj::strTree("StructList(size,", typeName(typeBody.getList().getElementType()),".STRUCT_SIZE)") :
             kj::strTree("List(", fieldSize, ", size)")),
            ");\n",
            spaces(indent), "  }\n"),

        kj::strTree(
            kind == FieldKind::STRUCT && !hasDiscriminantValue(proto)
            ? kj::strTree(
              "  inline ", type, "::Pipeline get", titleCase, "();\n")
            : kj::strTree()),

        kj::strTree(
            kj::mv(unionDiscrim.isDefs),
            "inline ", type, "::Reader ", scope, "Reader::get", titleCase, "() const {\n",
            unionDiscrim.check,
            "  return ::capnp::_::PointerHelpers<", type, ">::get(\n"
            "      _reader.getPointerField(", offset, " * ::capnp::POINTERS)", defaultParam, ");\n"
            "}\n"
            "inline ", type, "::Builder ", scope, "Builder::get", titleCase, "() {\n",
            unionDiscrim.check,
            "  return ::capnp::_::PointerHelpers<", type, ">::get(\n"
            "      _builder.getPointerField(", offset, " * ::capnp::POINTERS)", defaultParam, ");\n"
            "}\n",
            kind == FieldKind::STRUCT && !hasDiscriminantValue(proto)
            ? kj::strTree(
              "inline ", type, "::Pipeline ", scope, "Pipeline::get", titleCase, "() {\n",
              "  return ", type, "::Pipeline(_typeless.getPointerField(", offset, "));\n"
              "}\n")
            : kj::strTree(),
            "inline void ", scope, "Builder::set", titleCase, "(", type, "::Reader value) {\n",
            unionDiscrim.set,
            "  ::capnp::_::PointerHelpers<", type, ">::set(\n"
            "      _builder.getPointerField(", offset, " * ::capnp::POINTERS), value);\n"
            "}\n",
            kind == FieldKind::LIST && !isStructOrCapList
            ? kj::strTree(
              "inline void ", scope, "Builder::set", titleCase, "(::kj::ArrayPtr<const ", elementReaderType, "> value) {\n",
              unionDiscrim.set,
              "  ::capnp::_::PointerHelpers<", type, ">::set(\n"
              "      _builder.getPointerField(", offset, " * ::capnp::POINTERS), value);\n"
              "}\n")
            : kj::strTree(),
            kind == FieldKind::STRUCT
            ? kj::strTree(
                "inline ", type, "::Builder ", scope, "Builder::init", titleCase, "() {\n",
                unionDiscrim.set,
                "  return ::capnp::_::PointerHelpers<", type, ">::init(\n"
                "      _builder.getPointerField(", offset, " * ::capnp::POINTERS));\n"
                "}\n")
            : kj::strTree(
              "inline ", type, "::Builder ", scope, "Builder::init", titleCase, "(unsigned int size) {\n",
              unionDiscrim.set,
              "  return ::capnp::_::PointerHelpers<", type, ">::init(\n"
              "      _builder.getPointerField(", offset, " * ::capnp::POINTERS), size);\n"
              "}\n"),
            "inline void ", scope, "Builder::adopt", titleCase, "(\n"
            "    ::capnp::Orphan<", type, ">&& value) {\n",
            unionDiscrim.set,
            "  ::capnp::_::PointerHelpers<", type, ">::adopt(\n"
            "      _builder.getPointerField(", offset, " * ::capnp::POINTERS), kj::mv(value));\n"
            "}\n"
            "inline ::capnp::Orphan<", type, "> ", scope, "Builder::disown", titleCase, "() {\n",
            unionDiscrim.check,
            "  return ::capnp::_::PointerHelpers<", type, ">::disown(\n"
            "      _builder.getPointerField(", offset, " * ::capnp::POINTERS));\n"
            "}\n"
            "\n")
      };
    } else {
      KJ_UNREACHABLE;
    }
  }

  // -----------------------------------------------------------------

  struct StructText {
    kj::StringTree outerTypeDef;
    kj::StringTree readerBuilderDefs;
    kj::StringTree inlineMethodDefs;
  };

  kj::StringTree makeReaderDef(kj::StringPtr fullName, kj::StringPtr unqualifiedParentType,
                               bool isUnion, uint discriminantOffset, kj::Array<kj::StringTree>&& methodDecls,
                               int indent) {
    return kj::strTree(
      spaces(indent), "public static final class Reader {\n",
      spaces(indent), "  public Reader(org.capnproto.StructReader base){ this._reader = base; }\n",
      "\n",
      (isUnion ?
       kj::strTree(spaces(indent), "  public Which which() {\n",
                   spaces(indent), "    return Which.values()[_reader.getShortField(",
                   discriminantOffset, ")];\n",
                   spaces(indent), "  }\n")
       : kj::strTree()),
      kj::mv(methodDecls),
      spaces(indent), "  public org.capnproto.StructReader _reader;\n",
      spaces(indent), "}\n"
      "\n");
  }

  kj::StringTree makeBuilderDef(kj::StringPtr fullName, kj::StringPtr unqualifiedParentType,
                                schema::Node::Struct::Reader structNode,
                                kj::Array<kj::StringTree>&& methodDecls,
                                int indent) {
    bool isUnion = structNode.getDiscriminantCount() != 0;
    return kj::strTree(
      spaces(indent), "public static final class Builder {\n",
      spaces(indent), "  public Builder(org.capnproto.StructBuilder base){ this._builder = base; }\n",
      spaces(indent), "  public org.capnproto.StructBuilder _builder;\n",
      (isUnion ?
       kj::strTree(spaces(indent), "  public Which which() {\n",
                   spaces(indent), "   return Which.values()[_builder.getShortField(",
                   structNode.getDiscriminantOffset(), ")];\n",
                   spaces(indent), "  }\n")
       : kj::strTree()),
      spaces(indent), "  public final Reader asReader() {\n",
      spaces(indent), "    return new Reader(this._builder.asReader());\n",
      spaces(indent), "  }\n",
      kj::mv(methodDecls),
      spaces(indent), "}\n",
      "\n");
  }

  StructText makeStructText(kj::StringPtr scope, kj::StringPtr name, StructSchema schema,
                            kj::Array<kj::StringTree> nestedTypeDecls, int indent) {
    auto proto = schema.getProto();
    auto fullName = kj::str(scope, name);
    auto subScope = kj::str(fullName, ".");
    auto fieldTexts = KJ_MAP(f, schema.getFields()) { return makeFieldText(subScope, f, indent + 1); };

    auto structNode = proto.getStruct();
    uint discrimOffset = structNode.getDiscriminantOffset();
    structNode.getPointerCount();

    return StructText {
      kj::strTree(
        spaces(indent), "public static class ", name, " {\n",
        kj::strTree(
          spaces(indent), "  public static final org.capnproto.StructSize STRUCT_SIZE =\n",
          spaces(indent), "    new org.capnproto.StructSize((short)", structNode.getDataWordCount(),
          ",(short)", structNode.getPointerCount(),
          ", org.capnproto.FieldSize.", FIELD_SIZE_NAMES[(int)structNode.getPreferredListEncoding()], ");\n"),

        spaces(indent), "  public static class Factory implements org.capnproto.StructFactory<Builder, Reader> {\n",
        spaces(indent),
        "    public final Reader fromStructReader(org.capnproto.StructReader reader) {\n",
        spaces(indent), "      return new Reader(reader);\n",
        spaces(indent), "    }\n",
        spaces(indent), "    public final Builder fromStructBuilder(org.capnproto.StructBuilder builder) {\n",
        spaces(indent), "      return new Builder(builder);\n",
        spaces(indent), "    }\n",
        spaces(indent), "    public final org.capnproto.StructSize structSize() {\n",
        spaces(indent), "      return ", fullName, ".STRUCT_SIZE;\n",
        spaces(indent), "    }\n",
        spaces(indent), "    public final Reader asReader(Builder builder) {\n",
        spaces(indent), "      return new Reader(builder._builder.asReader());\n",
        spaces(indent), "    }\n",

        spaces(indent), "  }\n",
        spaces(indent), "  public static final Factory factory = new Factory();\n",


        kj::strTree(makeReaderDef(fullName, name, structNode.getDiscriminantCount() != 0,
                                  structNode.getDiscriminantOffset(),
                                  KJ_MAP(f, fieldTexts) { return kj::mv(f.readerMethodDecls); },
                                  indent + 1),
                    makeBuilderDef(fullName, name, structNode,
                                   KJ_MAP(f, fieldTexts) { return kj::mv(f.builderMethodDecls); },
                                   indent + 1)),

        structNode.getDiscriminantCount() == 0 ?
        kj::strTree() :
        kj::strTree(
          spaces(indent), "  public enum Which {\n",
          KJ_MAP(f, structNode.getFields()) {
            if (hasDiscriminantValue(f)) {
              return kj::strTree(spaces(indent), "    ", toUpperCase(f.getName()), ",\n");
            } else {
              return kj::strTree();
            }
          },
          spaces(indent), "  }\n"),
        KJ_MAP(n, nestedTypeDecls) { return kj::mv(n); },
        spaces(indent), "}\n"
        "\n",
        "\n"),

        kj::strTree(),
        kj::strTree()
        };
  }


  // -----------------------------------------------------------------

  struct ConstText {
    bool needsSchema;
    kj::StringTree decl;
    kj::StringTree def;
  };

  ConstText makeConstText(kj::StringPtr scope, kj::StringPtr name, ConstSchema schema) {
    auto proto = schema.getProto();
    auto constProto = proto.getConst();
    auto type = constProto.getType();
    auto typeName_ = typeName(type).flatten();
    auto upperCase = toUpperCase(name);

    // Linkage qualifier for non-primitive types.
    const char* linkage = scope.size() == 0 ? "extern " : "static ";

    switch (type.which()) {
      case schema::Value::VOID:
      case schema::Value::BOOL:
      case schema::Value::INT8:
      case schema::Value::INT16:
      case schema::Value::INT32:
      case schema::Value::INT64:
      case schema::Value::UINT8:
      case schema::Value::UINT16:
      case schema::Value::UINT32:
      case schema::Value::UINT64:
      case schema::Value::FLOAT32:
      case schema::Value::FLOAT64:
      case schema::Value::ENUM:
        return ConstText {
          false,
          kj::strTree("public static final ", typeName_, ' ', upperCase, " = ",
              literalValue(constProto.getType(), constProto.getValue()), ";\n"),
          scope.size() == 0 ? kj::strTree() : kj::strTree(
              "final ", typeName_, ' ', scope, upperCase, ";\n")
        };

      case schema::Value::TEXT: {
        kj::String constType = kj::strTree(
            "::capnp::_::ConstText<", schema.as<Text>().size(), ">").flatten();
        return ConstText {
          true,
          kj::strTree(linkage, "const ", constType, ' ', upperCase, ";\n"),
          kj::strTree("const ", constType, ' ', scope, upperCase, "(::capnp::schemas::b_",
                      kj::hex(proto.getId()), ".words + ", schema.getValueSchemaOffset(), ");\n")
        };
      }

      case schema::Value::DATA: {
        kj::String constType = kj::strTree(
            "::capnp::_::ConstData<", schema.as<Data>().size(), ">").flatten();
        return ConstText {
          true,
          kj::strTree(linkage, "const ", constType, ' ', upperCase, ";\n"),
          kj::strTree("const ", constType, ' ', scope, upperCase, "(::capnp::schemas::b_",
                      kj::hex(proto.getId()), ".words + ", schema.getValueSchemaOffset(), ");\n")
        };
      }

      case schema::Value::STRUCT: {
        kj::String constType = kj::strTree(
            "::capnp::_::ConstStruct<", typeName_, ">").flatten();
        return ConstText {
          true,
          kj::strTree(linkage, "const ", constType, ' ', upperCase, ";\n"),
          kj::strTree("const ", constType, ' ', scope, upperCase, "(::capnp::schemas::b_",
                      kj::hex(proto.getId()), ".words + ", schema.getValueSchemaOffset(), ");\n")
        };
      }

      case schema::Value::LIST: {
        kj::String constType = kj::strTree(
            "::capnp::_::ConstList<", typeName(type.getList().getElementType()), ">").flatten();
        return ConstText {
          true,
          kj::strTree(linkage, "const ", constType, ' ', upperCase, ";\n"),
          kj::strTree("const ", constType, ' ', scope, upperCase, "(::capnp::schemas::b_",
                      kj::hex(proto.getId()), ".words + ", schema.getValueSchemaOffset(), ");\n")
        };
      }

      case schema::Value::ANY_POINTER:
      case schema::Value::INTERFACE:
        return ConstText { false, kj::strTree(), kj::strTree() };
    }

    KJ_UNREACHABLE;
  }

  // -----------------------------------------------------------------

  struct NodeText {
    kj::StringTree outerTypeDef;
    kj::StringTree readerBuilderDefs;
    kj::StringTree inlineMethodDefs;
    kj::StringTree capnpSchemaDefs;
    kj::StringTree capnpPrivateDecls;
    kj::StringTree capnpPrivateDefs;
    kj::StringTree sourceFileDefs;
  };

  struct NodeTextNoSchema {
    kj::StringTree outerTypeDef;
    kj::StringTree readerBuilderDefs;
    kj::StringTree inlineMethodDefs;
    kj::StringTree capnpPrivateDecls;
    kj::StringTree capnpPrivateDefs;
    kj::StringTree sourceFileDefs;
  };

  NodeText makeNodeText(kj::StringPtr scope,
                        kj::StringPtr name, Schema schema,
                        int indent) {
    auto proto = schema.getProto();
    auto fullName = kj::str(scope, name);
    auto subScope = kj::str(fullName, ".");
    auto hexId = kj::hex(proto.getId());

    // Compute nested nodes, including groups.
    kj::Vector<NodeText> nestedTexts(proto.getNestedNodes().size());
    for (auto nested: proto.getNestedNodes()) {
      nestedTexts.add(makeNodeText(
                                   subScope, nested.getName(), schemaLoader.get(nested.getId()), indent + 1));
    };

    if (proto.isStruct()) {
      for (auto field: proto.getStruct().getFields()) {
        if (field.isGroup()) {
          nestedTexts.add(makeNodeText(subScope, toTitleCase(field.getName()),
              schemaLoader.get(field.getGroup().getTypeId()), indent + 1));
        }
      }
    } else if (proto.isInterface()) {
      KJ_FAIL_REQUIRE("interfaces not implemented");
    }

    // Convert the encoded schema to a literal byte array.
    kj::ArrayPtr<const word> rawSchema = schema.asUncheckedMessage();
    auto schemaLiteral = kj::StringTree(KJ_MAP(w, rawSchema) {
      const byte* bytes = reinterpret_cast<const byte*>(&w);

      return kj::strTree(
        "\"",
        KJ_MAP(i, kj::range<uint>(0, sizeof(word))) {
          switch(bytes[i]) {
          case 0x0a:
            return kj::strTree("\\n");
          case 0x0d:
            return kj::strTree("\\r");
          case 0x22:
            return kj::strTree("\\\"");
          case 0x5c:
            return kj::strTree("\\\\");
          default:
            auto text = kj::hex(bytes[i]);
            return kj::strTree("\\u", kj::repeat('0', 4 - text.size()), text);
          }
        },
        "\" +");
    }, "\n   ");

    std::set<uint64_t> deps;
    enumerateDeps(proto, deps);

    kj::Array<uint> membersByName;
    kj::Array<uint> membersByDiscrim;
    switch (proto.which()) {
      case schema::Node::STRUCT: {
        auto structSchema = schema.asStruct();
        membersByName = makeMembersByName(structSchema.getFields());
        auto builder = kj::heapArrayBuilder<uint>(structSchema.getFields().size());
        for (auto field: structSchema.getUnionFields()) {
          builder.add(field.getIndex());
        }
        for (auto field: structSchema.getNonUnionFields()) {
          builder.add(field.getIndex());
        }
        membersByDiscrim = builder.finish();
        break;
      }
      case schema::Node::ENUM:
        membersByName = makeMembersByName(schema.asEnum().getEnumerants());
        break;
      case schema::Node::INTERFACE:
        membersByName = makeMembersByName(schema.asInterface().getMethods());
        break;
      default:
        break;
    }

    // Java limits method code size to 64KB. Maybe we should use class.getResource()?
    auto schemaDef = kj::strTree(
      "public static final byte[] b_", hexId, " = org.capnproto.GeneratedClassSupport.decodeRawBytes(\n",
      "   ", kj::mv(schemaLiteral), " \"\"",
      ");\n");
/*
        deps.size() == 0 ? kj::strTree() : kj::strTree(
            "static const ::capnp::_::RawSchema* const d_", hexId, "[] = {\n",
            KJ_MAP(depId, deps) {
              return kj::strTree("  &s_", kj::hex(depId), ",\n");
            },
            "};\n"),
        membersByName.size() == 0 ? kj::strTree() : kj::strTree(
            "static const uint16_t m_", hexId, "[] = {",
            kj::StringTree(KJ_MAP(index, membersByName) { return kj::strTree(index); }, ", "),
            "};\n"),
        membersByDiscrim.size() == 0 ? kj::strTree() : kj::strTree(
            "static const uint16_t i_", hexId, "[] = {",
            kj::StringTree(KJ_MAP(index, membersByDiscrim) { return kj::strTree(index); }, ", "),
            "};\n"),
        "const ::capnp::_::RawSchema s_", hexId, " = {\n"
        "  0x", hexId, ", b_", hexId, ".words, ", rawSchema.size(), ", ",
        deps.size() == 0 ? kj::strTree("nullptr") : kj::strTree("d_", hexId), ", ",
        membersByName.size() == 0 ? kj::strTree("nullptr") : kj::strTree("m_", hexId), ",\n",
        "  ", deps.size(), ", ", membersByName.size(), ", ",
        membersByDiscrim.size() == 0 ? kj::strTree("nullptr") : kj::strTree("i_", hexId),
        ", nullptr, nullptr\n"
        "};\n");*/

    NodeTextNoSchema top = makeNodeTextWithoutNested(
        scope, name, schema,
        KJ_MAP(n, nestedTexts) { return kj::mv(n.outerTypeDef); }, indent);

    return NodeText {
      kj::strTree(
          kj::mv(top.outerTypeDef),
          KJ_MAP(n, nestedTexts) { return kj::mv(n.outerTypeDef); }),

      kj::strTree(
          kj::mv(top.readerBuilderDefs),
          KJ_MAP(n, nestedTexts) { return kj::mv(n.readerBuilderDefs); }),

      kj::strTree(
          kj::mv(top.inlineMethodDefs),
          KJ_MAP(n, nestedTexts) { return kj::mv(n.inlineMethodDefs); }),

      kj::strTree(
          kj::mv(schemaDef),
          KJ_MAP(n, nestedTexts) { return kj::mv(n.capnpSchemaDefs); }),

      kj::strTree(
          kj::mv(top.capnpPrivateDecls),
          KJ_MAP(n, nestedTexts) { return kj::mv(n.capnpPrivateDecls); }),

      kj::strTree(
          kj::mv(top.capnpPrivateDefs),
          KJ_MAP(n, nestedTexts) { return kj::mv(n.capnpPrivateDefs); }),

      kj::strTree(
          kj::mv(top.sourceFileDefs),
          KJ_MAP(n, nestedTexts) { return kj::mv(n.sourceFileDefs); }),
    };
  }

  NodeTextNoSchema makeNodeTextWithoutNested(kj::StringPtr scope,
                                             kj::StringPtr name, Schema schema,
                                             kj::Array<kj::StringTree> nestedTypeDecls,
                                             int indent) {
    auto proto = schema.getProto();
    auto fullName = kj::str(scope, name);
    auto hexId = kj::hex(proto.getId());

    switch (proto.which()) {
      case schema::Node::FILE:
        KJ_FAIL_REQUIRE("This method shouldn't be called on file nodes.");

      case schema::Node::STRUCT: {
        StructText structText =
          makeStructText(scope, name, schema.asStruct(), kj::mv(nestedTypeDecls), indent);
        auto structNode = proto.getStruct();

        return NodeTextNoSchema {
          kj::mv(structText.outerTypeDef),
          kj::mv(structText.readerBuilderDefs),
          kj::mv(structText.inlineMethodDefs),

            kj::strTree(),
            kj::strTree(),

          kj::strTree(),
        };
      }

      case schema::Node::ENUM: {
        auto enumerants = schema.asEnum().getEnumerants();

        return NodeTextNoSchema {
          kj::strTree(
                      spaces(indent), "public enum ", name, " {\n",
                      KJ_MAP(e, enumerants) {
                        return kj::strTree(spaces(indent), "  ", toUpperCase(e.getProto().getName()), ",\n");
                      },
                      spaces(indent), "}\n"
              "\n"),

          kj::strTree(),
          kj::strTree(),

           kj::strTree(),
           kj::strTree(),

          kj::strTree(),
        };
      }

      case schema::Node::INTERFACE: {
        hasInterfaces = true;
        KJ_FAIL_REQUIRE("unimplemented");
      }

      case schema::Node::CONST: {
        auto constText = makeConstText(scope, name, schema.asConst());

        return NodeTextNoSchema {
          kj::strTree("  ", kj::mv(constText.decl)),
          kj::strTree(),
          kj::strTree(),

          kj::strTree(),
          kj::strTree(),

          kj::mv(constText.def),
        };
      }

      case schema::Node::ANNOTATION: {
        return NodeTextNoSchema {
          kj::strTree(),
          kj::strTree(),
          kj::strTree(),
          kj::strTree(),
          kj::strTree(),
          kj::strTree(),
        };
      }
    }

    KJ_UNREACHABLE;
  }

  // -----------------------------------------------------------------

  struct FileText {
    kj::String outerClassname;
    kj::StringTree source;
  };

  FileText makeFileText(Schema schema,
                        schema::CodeGeneratorRequest::RequestedFile::Reader request) {
    usedImports.clear();

    auto node = schema.getProto();
    auto displayName = node.getDisplayName();

    kj::StringPtr packageName;
    kj::StringPtr outerClassname;

    for (auto annotation: node.getAnnotations()) {
      if (annotation.getId() == PACKAGE_ANNOTATION_ID) {
        packageName = annotation.getValue().getText();
      } else if (annotation.getId() == OUTER_CLASSNAME_ANNOTATION_ID) {
        outerClassname = annotation.getValue().getText();
      }
    }

    if (packageName.size() == 0) {
      context.exitError(kj::str(displayName, ": must provide a Java package name."));
    }
    if (outerClassname.size() == 0) {
      context.exitError(kj::str(displayName, ": must provide a Java outer classname."));
    }

    auto nodeTexts = KJ_MAP(nested, node.getNestedNodes()) {
      return makeNodeText("", nested.getName(), schemaLoader.get(nested.getId()), 1);
    };

    kj::String separator = kj::str("// ", kj::repeat('=', 87), "\n");

    kj::Vector<kj::StringPtr> includes;
    for (auto import: request.getImports()) {
      if (usedImports.count(import.getId()) > 0) {
        includes.add(import.getName());
      }
    }

    kj::StringTree sourceDefs = kj::strTree(
        KJ_MAP(n, nodeTexts) { return kj::mv(n.sourceFileDefs); });

    return FileText {
      kj::str(outerClassname),
      kj::strTree(
          "// Generated by Cap'n Proto compiler, DO NOT EDIT\n"
          "// source: ", baseName(displayName), "\n\n",
          "package ", packageName, ";\n\n",
          //"import org.capnproto;\n",
          "public final class ", outerClassname, " {\n",
          KJ_MAP(n, nodeTexts) { return kj::mv(n.outerTypeDef); },
          "\n",
          "public static final class Schemas {\n",
          KJ_MAP(n, nodeTexts) { return kj::mv(n.capnpSchemaDefs); },
          "}\n",
          "}\n",
          "\n")
    };
  }

  // -----------------------------------------------------------------

  void makeDirectory(kj::StringPtr path) {
    KJ_IF_MAYBE(slashpos, path.findLast('/')) {
      // Make the parent dir.
      makeDirectory(kj::str(path.slice(0, *slashpos)));
    }

    if (mkdir(path.cStr(), 0777) < 0) {
      int error = errno;
      if (error != EEXIST) {
        KJ_FAIL_SYSCALL("mkdir(path)", error, path);
      }
    }
  }

  void writeFile(kj::StringPtr filename, const kj::StringTree& text) {
    if (!filename.startsWith("/")) {
      KJ_IF_MAYBE(slashpos, filename.findLast('/')) {
        // Make the parent dir.
        makeDirectory(kj::str(filename.slice(0, *slashpos)));
      }
    }

    int fd;
    KJ_SYSCALL(fd = open(filename.cStr(), O_CREAT | O_WRONLY | O_TRUNC, 0666), filename);
    kj::FdOutputStream out((kj::AutoCloseFd(fd)));

    text.visit(
        [&](kj::ArrayPtr<const char> text) {
          out.write(text.begin(), text.size());
        });
  }

  kj::MainBuilder::Validity run() {
    ReaderOptions options;
    options.traversalLimitInWords = 1 << 30;  // Don't limit.
    StreamFdMessageReader reader(STDIN_FILENO, options);
    auto request = reader.getRoot<schema::CodeGeneratorRequest>();

    for (auto node: request.getNodes()) {
      schemaLoader.load(node);
    }

    kj::FdOutputStream rawOut(STDOUT_FILENO);
    kj::BufferedOutputStreamWrapper out(rawOut);

    for (auto requestedFile: request.getRequestedFiles()) {
      auto schema = schemaLoader.get(requestedFile.getId());

      auto filename = requestedFile.getFilename();
      size_t stemstart = 0;
      size_t stemend = filename.size();
      KJ_IF_MAYBE(slashpos, filename.findLast('/')) {
        stemstart = *slashpos + 1;
      }
      KJ_IF_MAYBE(dotpos, filename.findLast('.')) {
        stemend = *dotpos;
      }

      auto fileText = makeFileText(schema, requestedFile);

      writeFile(kj::str(filename.slice(0, stemstart), fileText.outerClassname, ".java"), fileText.source);
    }

    return true;
  }
};

}  // namespace
}  // namespace capnp

KJ_MAIN(capnp::CapnpcJavaMain);
