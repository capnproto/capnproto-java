package org.capnproto.examples;

import org.capnproto.MessageBuilder;
import org.capnproto.MessageReader;
import org.capnproto.StructList;
import org.capnproto.InputStreamMessageReader;
import org.capnproto.Text;

import org.capnproto.examples.Addressbook.*;

public class AddressbookMain {

    public static void writeAddressBook() {
        System.out.println("WARNING: writing is not yet fully implemented");
        MessageBuilder message = new MessageBuilder();
        AddressBook.Builder addressbook = message.initRoot(AddressBook.Builder.factory);
        StructList.Builder<Person.Builder> people = addressbook.initPeople(2);

        Person.Builder alice = people.get(0);
        alice.setId(123);
        alice.setName(new Text.Reader("Alice"));
        alice.setEmail(new Text.Reader("alice@example.com"));

        StructList.Builder<Person.PhoneNumber.Builder> alicePhones = alice.initPhones(1);
        alicePhones.get(0).setNumber(new Text.Reader("555-1212"));
        alicePhones.get(0).setType(Person.PhoneNumber.Type.MOBILE);
        alice.getEmployment().setSchool(new Text.Reader("MIT"));

        Person.Builder bob = people.get(0);
        bob.setId(456);
        bob.setName(new Text.Reader("Bob"));
        bob.setEmail(new Text.Reader("bob@example.com"));
        StructList.Builder<Person.PhoneNumber.Builder> bobPhones = bob.initPhones(2);
        bobPhones.get(0).setNumber(new Text.Reader("555-4567"));
        bobPhones.get(0).setType(Person.PhoneNumber.Type.HOME);
        bobPhones.get(1).setNumber(new Text.Reader("555-7654"));
        bobPhones.get(1).setType(Person.PhoneNumber.Type.WORK);
        bob.getEmployment().setUnemployed();
    }

    public static void printAddressBook() throws java.io.IOException {
        MessageReader message = InputStreamMessageReader.create(System.in);
        AddressBook.Reader addressbook = message.getRoot(AddressBook.Reader.factory);
        StructList.Reader<Person.Reader> people = addressbook.getPeople();
        int size = people.size();
        for(int ii = 0; ii < size; ++ii) {
            Person.Reader person = people.get(ii);
            System.out.println(person.getName() + ": " + person.getEmail());

            StructList.Reader<Person.PhoneNumber.Reader> phones = person.getPhones();
            for (int jj = 0; jj < phones.size(); ++jj) {
                Person.PhoneNumber.Reader phone = phones.get(jj);
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
