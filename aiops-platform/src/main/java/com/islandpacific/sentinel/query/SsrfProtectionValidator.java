package com.islandpacific.sentinel.query;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * SSRF Protection Validator with DNS Pinning & Multi-Answer Verification.
 * Enforces strict protocol, port capability, and IP range validation.
 */
@Component
public class SsrfProtectionValidator {

    @Value("${sentinel.datasource.ssrf.allow-private-ips:false}")
    private boolean allowPrivateIps;

    private static final Set<Integer> PROMETHEUS_PORTS = Set.of(80, 443, 9090);
    private static final Set<Integer> LOKI_PORTS = Set.of(80, 443, 3100);
    private static final Set<Integer> THANOS_PORTS = Set.of(80, 443, 9090, 10901, 10902);

    public ValidatedEndpoint validateAndPinUrl(String urlStr, String kind) {
        if (urlStr == null || urlStr.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or empty");
        }

        URI uri;
        try {
            uri = URI.create(urlStr.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + urlStr);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new SecurityException("SSRF Blocked: Only http and https protocols are allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SecurityException("SSRF Blocked: URL host is missing");
        }

        // Check embedded user info / credentials in URL
        if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
            throw new SecurityException("SSRF Blocked: User credentials embedded in URL authority are prohibited");
        }

        // Check localhost aliases
        String lowerHost = host.toLowerCase();
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost") || lowerHost.equals("localhost.localdomain")) {
            throw new SecurityException("SSRF Blocked: Localhost domain alias is prohibited");
        }

        int port = uri.getPort();
        if (port == -1) {
            port = scheme.equalsIgnoreCase("https") ? 443 : 80;
        }

        // Port & Kind Capability validation
        validatePortForKind(port, kind);

        // Check decimal / hex / octal IP notation
        checkNumericIpRepresentation(lowerHost);

        // Resolve hostname to IP addresses (DNS resolution check)
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("SSRF Blocked: Unable to resolve hostname " + host);
        }

        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("SSRF Blocked: No IP address resolved for " + host);
        }

        // Check EACH resolved IP address. If ANY address is private/loopback/metadata, block whole request!
        for (InetAddress addr : addresses) {
            validateIpAddress(addr);
        }

        // Return validated endpoint containing original URI, hostname, port, scheme, and validated IP address
        return new ValidatedEndpoint(uri, host, port, scheme, addresses[0]);
    }

    public void validateUrl(String urlStr, String kind) {
        validateAndPinUrl(urlStr, kind);
    }

    public static class ValidatedEndpoint {
        private final URI originalUri;
        private final String hostname;
        private final int port;
        private final String scheme;
        private final InetAddress validatedAddress;

        public ValidatedEndpoint(URI originalUri, String hostname, int port, String scheme, InetAddress validatedAddress) {
            this.originalUri = originalUri;
            this.hostname = hostname;
            this.port = port;
            this.scheme = scheme;
            this.validatedAddress = validatedAddress;
        }

        public URI getOriginalUri() { return originalUri; }
        public String getHostname() { return hostname; }
        public int getPort() { return port; }
        public String getScheme() { return scheme; }
        public InetAddress getValidatedAddress() { return validatedAddress; }
    }

    private void validatePortForKind(int port, String kind) {
        if (kind == null) return;
        String normalizedKind = kind.toLowerCase();

        Set<Integer> allowedPorts;
        switch (normalizedKind) {
            case "prometheus":
                allowedPorts = PROMETHEUS_PORTS;
                break;
            case "loki":
                allowedPorts = LOKI_PORTS;
                break;
            case "thanos":
                allowedPorts = THANOS_PORTS;
                break;
            default:
                allowedPorts = Set.of(80, 443, 9090, 3100, 10901, 10902);
                break;
        }

        if (!allowedPorts.contains(port)) {
            throw new SecurityException("SSRF Blocked: Port " + port + " is not permitted for datasource kind " + kind);
        }
    }

    private void checkNumericIpRepresentation(String host) {
        // Hex e.g., 0x7f000001
        if (host.startsWith("0x") || host.startsWith("0X")) {
            throw new SecurityException("SSRF Blocked: Hexadecimal IP representation is prohibited");
        }
        // Pure decimal integer e.g., 2130706433
        if (host.matches("^\\d+$")) {
            throw new SecurityException("SSRF Blocked: Decimal integer IP representation is prohibited");
        }
        // Octal notation e.g., 0177.0.0.1 or octets with leading zeroes
        if (host.matches("^(0[0-7]+|\\d+)\\.(0[0-7]+|\\d+)\\.(0[0-7]+|\\d+)\\.(0[0-7]+|\\d+)$")) {
            String[] parts = host.split("\\.");
            for (String p : parts) {
                if (p.length() > 1 && p.startsWith("0")) {
                    throw new SecurityException("SSRF Blocked: Octal IP representation is prohibited");
                }
            }
        }
    }

    private void validateIpAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()) {
            throw new SecurityException("SSRF Blocked: Loopback address " + addr.getHostAddress() + " is prohibited");
        }

        if (addr.isAnyLocalAddress()) {
            throw new SecurityException("SSRF Blocked: AnyLocal (0.0.0.0 / ::) address is prohibited");
        }

        if (addr.isLinkLocalAddress()) {
            throw new SecurityException("SSRF Blocked: Link-local address " + addr.getHostAddress() + " is prohibited");
        }

        if (addr.isMulticastAddress()) {
            throw new SecurityException("SSRF Blocked: Multicast address " + addr.getHostAddress() + " is prohibited");
        }

        byte[] bytes = addr.getAddress();

        // Handle IPv4-mapped IPv6 address e.g. ::ffff:127.0.0.1
        if (addr instanceof Inet6Address && bytes.length == 16) {
            boolean isIPv4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) { isIPv4Mapped = false; break; }
            }
            if (isIPv4Mapped && (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF) {
                byte[] ipv4Bytes = new byte[4];
                System.arraycopy(bytes, 12, ipv4Bytes, 0, 4);
                try {
                    InetAddress mappedIpv4 = InetAddress.getByAddress(ipv4Bytes);
                    validateIpAddress(mappedIpv4);
                    return;
                } catch (UnknownHostException e) {
                    throw new SecurityException("SSRF Blocked: Invalid IPv4-mapped IPv6 address");
                }
            }
        }

        // Check Cloud Metadata IP: 169.254.169.254
        if (bytes.length == 4 && (bytes[0] & 0xFF) == 169 && (bytes[1] & 0xFF) == 254) {
            throw new SecurityException("SSRF Blocked: Cloud metadata address 169.254.x.x is strictly prohibited");
        }

        // Check RFC 1918 private ranges unless explicitly allowed
        if (!allowPrivateIps) {
            if (addr.isSiteLocalAddress()) {
                throw new SecurityException("SSRF Blocked: Private IP address " + addr.getHostAddress() + " is prohibited");
            }
            if (bytes.length == 16) {
                int b0 = bytes[0] & 0xFF;
                if ((b0 & 0xFE) == 0xFC) {
                    throw new SecurityException("SSRF Blocked: Private Unique Local IPv6 address (fc00::/7) is prohibited");
                }
            }
            if (bytes.length == 4) {
                int b0 = bytes[0] & 0xFF;
                int b1 = bytes[1] & 0xFF;

                // 10.0.0.0/8
                if (b0 == 10) {
                    throw new SecurityException("SSRF Blocked: Private IP 10.x.x.x is prohibited");
                }
                // 172.16.0.0/12
                if (b0 == 172 && (b1 >= 16 && b1 <= 31)) {
                    throw new SecurityException("SSRF Blocked: Private IP 172.16-31.x.x is prohibited");
                }
                // 192.168.0.0/16
                if (b0 == 192 && b1 == 168) {
                    throw new SecurityException("SSRF Blocked: Private IP 192.168.x.x is prohibited");
                }
                // 127.0.0.0/8
                if (b0 == 127) {
                    throw new SecurityException("SSRF Blocked: Loopback IP 127.x.x.x is prohibited");
                }
            }
        }
    }
}
