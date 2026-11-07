package com.qualcomm.robotcore.hardware;

import java.util.LinkedHashMap;
import java.util.Map;

/** Sim stand-in for the FTC SDK HardwareMap. */
public class HardwareMap {

  private final Map<String, HardwareDevice> devices = new LinkedHashMap<>();

  public void put(String deviceName, HardwareDevice device) {
    devices.put(deviceName.trim(), device);
  }

  public <T> T get(Class<? extends T> classOrInterface, String deviceName) {
    deviceName = deviceName.trim();
    HardwareDevice device = devices.get(deviceName);
    if (device != null && classOrInterface.isInstance(device)) {
      return classOrInterface.cast(device);
    }
    throw new IllegalArgumentException(String.format(
        "Unable to find a hardware device with name \"%s\" and type %s",
        deviceName, classOrInterface.getSimpleName()));
  }
}
