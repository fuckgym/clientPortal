//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lx.gw;

import com.lx.gw.utils.OptionUtils;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.InputStream;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.yaml.snakeyaml.Yaml;

public class GatewayStart {
    private static Config config = new Config();
    private static final int EXIT_FAILED = 1;
    private static final Logger logger = LoggerFactory.getLogger(GatewayStart.class);
    private static final Options options = new Options();

    public GatewayStart() {
    }

    public static void main(String[] args) {
        initCmdLineOptions();
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            logger.error("unrecognized option: {}", new Object[]{e.getMessage()});
            printHelp();
        } catch (Exception var21) {
            printHelp();
        }

        if (cmd != null && !cmd.hasOption("h")) {
            String file = cmd.getOptionValue("conf", "conf.yaml");
            if (!file.isEmpty()) {
                try (InputStream in = ClassLoader.getSystemResourceAsStream(file)) {
                    Yaml yaml = new Yaml();
                    config = (Config)yaml.loadAs(in, Config.class);
                } catch (Exception e) {
                    System.err.println("Failed to load configuration file: " + file + "\n" + e.getMessage());
                    failed("loading configuration file error:" + e.getMessage());
                }
            }

            String host = cmd.getOptionValue("host", "");
            if (!host.isEmpty()) {
                config.setProxyRemoteHost(host);
            }

            if (cmd.hasOption("port")) {
                try {
                    int port = ((Number)cmd.getParsedOptionValue("port")).intValue();
                    config.setListenPort(port);
                } catch (ParseException var17) {
                    System.err.println("failed to parse port value");
                }
            }

            if (cmd.hasOption("nossl")) {
                config.setListenSsl(false);
            }

            Vertx vertx = Vertx.vertx(config.getVertxOptions());
            OptionUtils.setSharedGlobalConfig(vertx, config);
            logger.info("config {} {}", new Object[]{file, (new Yaml()).dump(config)});
            vertx.deployVerticle(new GatewayVerticle());
        } else {
            printHelp();
        }
    }

    private static void failed(String err, Object... args) {
        logger.error(err, args);
        System.exit(1);
    }

    private static void initCmdLineOptions() {
        options.addOption("h", "help", false, "print this message");
        Options var10000 = options;
        OptionBuilder.withLongOpt("nossl");
        OptionBuilder.hasArg(false);
        OptionBuilder.withDescription("Disable SSL (not recommended)");
        var10000.addOption(OptionBuilder.create());
        options.addOption("c", "conf", true, "Specify a YAML file to load configuration options from");
        var10000 = options;
        OptionBuilder.withLongOpt("port");
        OptionBuilder.hasArg();
        OptionBuilder.withType(Number.class);
        OptionBuilder.withDescription("Listen on port");
        var10000.addOption(OptionBuilder.create());
        options.addOption((String)null, "remoteProxyHost", true, "remote host, e.g www.interactivebrokers.com");
    }

    private static void printHelp() {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("java -jar gw.jar " + GatewayStart.class.getSimpleName(), options);
        System.exit(1);
    }
}
