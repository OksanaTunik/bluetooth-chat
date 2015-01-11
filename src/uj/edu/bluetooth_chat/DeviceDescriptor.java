package uj.edu.bluetooth_chat;

/**
 * Created by shybovycha on 11.01.15.
 */
public class DeviceDescriptor {
    protected String name;
    protected String address;
    protected Boolean paired;

    public DeviceDescriptor() {
        this.paired = false;
    }

    public DeviceDescriptor(String name, String address, Boolean paired) {
        this.name = name;
        this.address = address;
        this.paired = paired;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getPaired() {
        return paired;
    }

    public void setPaired(Boolean paired) {
        this.paired = paired;
    }
}
