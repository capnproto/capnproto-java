public class AddressbookMain {

    public static void writeAddressBook() {
        System.out.println("writing addressbook ...");
    }

    public static void printAddressBook() {
        System.out.println("printing addressbook ...");
    }

    public static void usage() {
        System.out.println("usage: addressbook [write | read]");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
        } else if (args[0].equals("write")) {
            writeAddressBook();
        } else if (args[0].equals("read")) {
            printAddressBook();
        } else {
            usage();
        }
    }
}
