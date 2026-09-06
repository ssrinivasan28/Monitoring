package com.islandpacific.monitoring.credtool;

import java.io.Console;

import com.islandpacific.monitoring.common.CredentialProtector;

/**
 * CLI tool to encrypt a credential with Windows DPAPI (machine scope).
 *
 * Usage:
 *   java -jar CredTool.jar            (opens UI; console prompt if headless)
 *   java -jar CredTool.jar <value>
 *
 * Run on the machine where the monitors will run, then paste the printed
 * DPAPI(...) value into the .properties file in place of the plaintext.
 */
public class MainCredTool {

    public static void main(String[] args) {
        String value;
        if (args.length > 0) {
            value = args[0];
        } else {
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                CredToolUI.launch();
                return;
            }
            Console console = System.console();
            if (console == null) {
                System.err.println("Usage: java -jar CredTool.jar <value-to-encrypt>");
                System.exit(1);
                return;
            }
            char[] chars = console.readPassword("Value to encrypt: ");
            if (chars == null || chars.length == 0) {
                System.err.println("No value entered.");
                System.exit(1);
                return;
            }
            value = new String(chars);
        }
        System.out.println(CredentialProtector.protect(value));
    }
}
