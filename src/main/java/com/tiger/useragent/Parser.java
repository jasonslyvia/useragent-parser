package com.tiger.useragent;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.tiger.useragent.Constant.DEFAULT_VALUE;

/**
 * com.tiger.useragent
 * author : zhaolihe
 * email : dayingzhaolihe@126.com
 * date : 2017/5/9
 */
public class Parser {
    private OsParser osParser;
    private BrowserParser browserParser;
    private DeviceParser deviceParser;
    private DeviceMap deviceMap;

    public static Map<String, Map<String, String>> mobileParser;

    public Parser() throws IOException {
        readConfigs();
    }

    private void readConfigs() throws IOException {

        mobileParser = MobileParser.mapForFile(Parser.class.getResourceAsStream("/MobileDictionary.txt"));

        Yaml yaml = new Yaml(new SafeConstructor());
        try (InputStream stream = Parser.class.getResourceAsStream("/OSConfig.yaml")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> osParserConfigs = (List<Map<String, String>>) yaml.load(stream);
            if (osParserConfigs == null) {
                throw new IllegalArgumentException("OSConfig.yaml loading failed.");
            }
            osParser = OsParser.fromList(osParserConfigs);
        }

        try (InputStream stream = Parser.class.getResourceAsStream("/BrowserConfig.yaml")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> browserParserConfigs = (List<Map<String, String>>) yaml.load(stream);
            if (browserParserConfigs == null) {
                throw new IllegalArgumentException("BrowserConfig.yaml loading failed.");
            }
            browserParser = BrowserParser.fromList(browserParserConfigs);
        }

        try (InputStream configStream = Parser.class.getResourceAsStream("/DeviceConfig.yaml")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> deviceParserConfigs = (List<Map<String, String>>) yaml.load(configStream);
            if (deviceParserConfigs == null) {
                throw new IllegalArgumentException("DeviceConfig.yaml loading failed.");
            }
            List<DevicePattern> patterns = DeviceParser.patternsFromList(deviceParserConfigs);
            deviceParser = new DeviceParser(patterns);
        }

        try (InputStream dictionaryStream = Parser.class.getResourceAsStream("/DeviceDictionary.txt")) {
            deviceMap = DeviceMap.mapFromFile(dictionaryStream);
        }
    }

    public UserAgentInfo parse(String agentString) {
        if (agentString == null) {
            return null;
        }

        Device device = parseDevice(agentString);
        if (device.deviceType.equals(DeviceType.Spider)) {
            return buildUserAgentInfo(Os.DEFAULT_OS, Browser.DEFAULT_BROWSER, device);
        }
        Os os = parseOS(agentString);
        Browser browser = parseBrowser(agentString);
        if (os == null) {
            os = Os.DEFAULT_OS;
        } else if (os.isTv) {
            device = Device.DEFAULT_TV;
        } else if (os.isMobile && !device.isMobile && !(device.deviceType == DeviceType.TV)) {
            device = Device.DEFAULT_PHONE_SCREEN;
        }

        return buildUserAgentInfo(os, browser, device);
    }

    public Device parseDevice(String agentString) {
        Device device = deviceParser.parse(agentString);
        return deviceMap.parseDevice(device);
    }

    public Browser parseBrowser(String agentString) {
        return browserParser.parse(agentString);
    }

    public Os parseOS(String agentString) {
        return osParser.parse(agentString);
    }

    private UserAgentInfo buildUserAgentInfo(Os os, Browser browser, Device device) {
        UserAgentInfo userAgentInfo = new UserAgentInfo();
        String detail;

        // OS to OsInfo
        if (StringUtils.isEmpty(os.major)) {
            detail = os.family;
        } else {
            detail = StringUtils.isEmpty(os.minor) ? os.family + " " + os.major
                    : os.family + " " + os.major + "." + os.minor;
        }
        userAgentInfo.setOsName(os.brand);
        userAgentInfo.setOs(detail);

        // Browser to BrowserInfo
        if (StringUtils.isEmpty(browser.major)) {
            detail = browser.family;
        } else if (StringUtils.isEmpty(browser.minor)) {
            detail = browser.family + " " + browser.major;
        } else if (!StringUtils.isEmpty(browser.patch)) {
            detail = browser.family + " " + browser.major + "." + browser.minor + "." + StringUtils.stripStart(browser.patch, ".");
        } else {
            detail = browser.family + " " + browser.major + "." + browser.minor;
        }
        userAgentInfo.setBrowserName(browser.brand);
        userAgentInfo.setBrowser(detail);

        // Device to DeviceInfo
        if (!device.brand.equalsIgnoreCase("PC") && !device.brand.equals(DEFAULT_VALUE) && !device.deviceType.equals(DeviceType.Spider)) {
            detail = device.brand + " " + device.family;
        } else {
            detail = device.family;
        }
        userAgentInfo.setDeviceBrand(device.brand);
        userAgentInfo.setDeviceName(detail);
        userAgentInfo.setDeviceType(device.deviceType.toString());
        return userAgentInfo;
    }
}
