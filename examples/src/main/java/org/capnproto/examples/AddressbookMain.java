package org.capnproto.examples;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileDescriptor;

import org.capnproto.MessageBuilder;
import org.capnproto.MessageReader;
import org.capnproto.SerializePacked;
import org.capnproto.StructList;

import org.capnproto.examples.Addressbook.*;

public class AddressbookMain {

    public static void writeAddressBook() throws java.io.IOException {
        MessageBuilder message = new MessageBuilder();
        AddressBook.Builder addressbook = message.initRoot(AddressBook.factory);
        StructList.Builder<Person.Builder> people = addressbook.initPeople(2);

        Person.Builder alice = people.get(0);
        alice.setId(123);
        alice.setName("Alice");
        alice.setEmail("alice@example.com");

        StructList.Builder<Person.PhoneNumber.Builder> alicePhones = alice.initPhones(1);
        alicePhones.get(0).setNumber("555-1212");
        alicePhones.get(0).setType(Person.PhoneNumber.Type.MOBILE);
        alice.getEmployment().setSchool("MIT");

        Person.Builder bob = people.get(1);
        bob.setId(456);
        bob.setName("Bob");
        bob.setEmail("bob@example.com");
        StructList.Builder<Person.PhoneNumber.Builder> bobPhones = bob.initPhones(2);
        bobPhones.get(0).setNumber("555-4567");
        bobPhones.get(0).setType(Person.PhoneNumber.Type.HOME);
        bobPhones.get(1).setNumber("555-7654");
        bobPhones.get(1).setType(Person.PhoneNumber.Type.WORK);
        bob.getEmployment().setUnemployed(org.capnproto.Void.VOID);

        SerializePacked.writeUnbuffered((new FileOutputStream(FileDescriptor.out)).getChannel(),
                                        message);
    }

    public static void printAddressBook() throws java.io.IOException {
        MessageReader message = SerializePacked.readUnbuffered(
            (new FileInputStream(FileDescriptor.in)).getChannel());
        AddressBook.Reader addressbook = message.getRoot(AddressBook.factory);
        for(Person.Reader person : addressbook.getPeople()) {
            System.out.println(person.getName() + ": " + person.getEmail());

            for (Person.PhoneNumber.Reader phone : person.getPhones()) {
                String typeName = "UNKNOWN";
                switch (phone.getType()) {
                case MOBILE :
                    typeName = "mobile";
                    break;
                case HOME :
                    typeName = "home";
                    break;
                case WORK :
                    typeName = "work";
                    break;
                }
                System.out.println("  " + typeName + " phone: " + phone.getNumber());
            }

            Person.Employment.Reader employment = person.getEmployment();
            switch (employment.which()) {
            case UNEMPLOYED :
                System.out.println("  unemployed");
                break;
            case EMPLOYER :
                System.out.println("  employer: " + employment.getEmployer());
                break;
            case SCHOOL :
                System.out.println("  student at: " + employment.getSchool());
                break;
            case SELF_EMPLOYED:
                System.out.println("  self-employed");
                break;
            default :
                break;
            }
        }
    }

    public static void usage() {
        System.out.println("usage: addressbook [write | read]");
    }

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                usage();
            } else if (args[0].equals("write")) {
                writeAddressBook();
            } else if (args[0].equals("read")) {
                printAddressBook();
            } else {
                usage();
            }
        } catch (java.io.IOException e) {
            System.out.println("io exception: "  + e);
        }
    }
}
