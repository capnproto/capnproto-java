
public class AddressbookMain {

    public static void writeAddressBook() {
        System.out.println("writing is yet unimplemented");
    }

    public static void printAddressBook() throws java.io.IOException {
        System.out.println("printing addressbook ...");
        capnp.MessageReader message = capnp.InputStreamMessageReader.create(System.in);
        Addressbook.AddressBook.Reader addressbook = message.getRoot(Addressbook.AddressBook.Reader.factory);
        capnp.StructList.Reader<Addressbook.Person> people = addressbook.getPeople();
        int size = people.size();
        for(int ii = 0; ii < size; ++ii) {
            people.get(ii);
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
