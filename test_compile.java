import java.net.InetAddress;
public class test_compile {
    public static void main(String[] args) throws Exception {
        String hostName = InetAddress.getLocalHost().getHostName();
        System.out.println(hostName);
    }
}
