package org.capnproto.examples;

import org.capnproto.MessageReader;
import org.capnproto.StructList;
import org.capnproto.InputStreamMessageReader;

public class AddressbookMain {

    public static void writeAddressBook() {
        System.out.println("writing is yet unimplemented");
    }

    public static void printAddressBook() throws java.io.IOException {
        MessageReader message = InputStreamMessageReader.create(System.in);
        Addressbook.AddressBook.Reader addressbook = message.getRoot(Addressbook.AddressBook.Reader.factory);
        StructList.Reader<Addressbook.Person.Reader> people = addressbook.getPeople();
        int size = people.size();
        for(int ii = 0; ii < size; ++ii) {
            Addressbook.Person.Reader person = people.get(ii);
            System.out.println(person.getName() + ": " + person.getEmail());

            StructList.Reader<Addressbook.Person.PhoneNumber.Reader> phones = person.getPhones();
            for (int jj = 0; jj < phones.size(); ++jj) {
                Addressbook.Person.PhoneNumber.Reader phone = phones.get(jj);
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

            Addressbook.Person.Employment.Reader employment = person.getEmployment();
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
