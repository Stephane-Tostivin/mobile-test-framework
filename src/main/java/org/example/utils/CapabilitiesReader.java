package org.example.utils;

import io.appium.java_client.remote.MobileCapabilityType;
import org.example.factory.DriverFactory;
import org.json.JSONObject;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CapabilitiesReader {

    // CONSTANTS
    private static final String CAPABILITIES_FILE = "/configuration/capabilities.json";

    // Logger
    static Logger logger = Logger.getLogger(CapabilitiesReader.class.getName());

    public static JSONObject getConfiguration() {
        String text = null;
        try {
            text = new String(Files.readAllBytes(Paths.get(DriverFactory.class.getResource(CAPABILITIES_FILE).toURI())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading the file: " + CAPABILITIES_FILE);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Error parsing the file: " + CAPABILITIES_FILE);
        }
        JSONObject configuration = new JSONObject(text);
        return configuration;
    }
    /**
     * Load from file capabilities.json the mobile capabilities
     * @return DesiredCapabilities
     */
    public static DesiredCapabilities getCaps(JSONObject config) {
        DesiredCapabilities caps = new DesiredCapabilities();

        // If proper system environment variable is defined, we take its value
        // So that the user or Jenkins can change the capabilities on the fly
        // Default values: capabilities file
        caps.setCapability(System.getProperty("PLATFORM_NAME", MobileCapabilityType.PLATFORM_NAME), config.get("platformName"));
        caps.setCapability(System.getProperty("PLATFORM_VERSION", MobileCapabilityType.PLATFORM_VERSION), config.get("platformVersion"));
        caps.setCapability(System.getProperty("DEVICE_NAME", MobileCapabilityType.DEVICE_NAME), config.get("deviceName"));

        // Set the package location as capability APP only if installation is required
        String installRequired = System.getProperty("TO_INSTALL");
        if(installRequired != null && !installRequired.isEmpty()) {
            logger.log(Level.INFO, "Capability APP set ==> new installation of the application");
            caps.setCapability(System.getProperty("APP_LOCATION", MobileCapabilityType.APP), config.get("appLocation"));
        }
        caps.setCapability("appActivity", config.get("appActivity"));
        caps.setCapability("appPackage", config.get("appPackage"));

        return caps;
    }


    /**
     * Build the URL for the Appium server
     * @return the URL for the targeted Appium server
     */
    public static String getAppiumHubURL(JSONObject config) {
        String AppiumHubURL = null;

        // Read the targeted System Under Test from the system environment variable TARGET_SUT
        // By default: local
        String targetSUT = System.getProperty("TARGET_SUT", "local");
        logger.log(Level.INFO, "The test system is: " + targetSUT);

        // Get the Appium URL if target is local
        if(targetSUT.equalsIgnoreCase("local")) {
            AppiumHubURL = config.getJSONObject("localEnv").getString("url");
        }

        // Get the Appium URL if target is BrowserStack
        else if(targetSUT.equalsIgnoreCase("browserstack")) {
            // If proper system environment variable is defined, we take the value
            // Otherwise we read the capabilities file
            String username = System.getProperty("BROWSERSTACK_USERNAME", config.getJSONObject("BrowserStackEnv").getString("username"));
            String accessKey = System.getProperty("BROWSERSTACK_ACCESS_KEY", config.getJSONObject("BrowserStackEnv").getString("access_key"));
            AppiumHubURL = "http://"+username+":"+accessKey+"@"+config.getJSONObject("BrowserStackEnv").getString("server")+"/wd/hub";
        }

        // Get the Appium URL as a local one if incorrect value for system variable has been given
        else {
            logger.log(Level.SEVERE, "The System Under Test targeted: " + targetSUT + " is unknown!");
            logger.log(Level.SEVERE, "Default local SUT will be considered instead");
            AppiumHubURL = config.getJSONObject("localEnv").getString("url");
        }

        logger.log(Level.INFO, "URL of the Appium server: " + AppiumHubURL);
        return AppiumHubURL;

    }

}
