package de.lippertmarkus.rapla2csv;

import org.apache.commons.cli.*;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.time.LocalDate;

public class Main
{
    private static Options exportOptions = new Options();
    private static Options additionalOptions = new Options();

    private static LocalDate timeFrom;
    private static LocalDate timeUntil;
    private static URIBuilder raplaLink;

    private static String fileName = "rapla.csv";

    public static void main(String[] args)
    {
        RaplaReader raplaReader;
        createCommandLineOptions();

        try {
            parseCommandLineOptions(args);
            raplaReader = new RaplaReader(timeFrom, timeUntil, raplaLink);
            raplaReader.getLessonsFromRapla();
            System.out.println(raplaReader.getExtractedLessonsInfo());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return; // abort export
        }

        // successfully extracted lessons, now export to CSV for calendar import
        try {
            raplaReader.exportToCSV(fileName);
            System.out.println("Export done: " + fileName);
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    private static void createCommandLineOptions()
    {
        // Additional options
        additionalOptions.addOption("h", "help", false, "Shows this help");
        additionalOptions.addOption("v", "version", false, "Show version number");

        // Program options
        exportOptions.addOption(Option.builder("f")
                .longOpt("from")
                .argName("date")
                .hasArg()
                .desc("Begin of the export time period, e.g. 2015-12-31")
                .required()
                .build()
        );
        exportOptions.addOption(Option.builder("u")
                .longOpt("until")
                .argName("date")
                .hasArg()
                .desc("End of the export time period, e.g. 2016-12-31")
                .required()
                .build()
        );
        exportOptions.addOption(Option.builder("l")
                .longOpt("link")
                .argName("link")
                .hasArg()
                .desc("Rapla link IN QUOTES, e.g. \"http://example.com/rapla?key=abc123\"")
                .required()
                .build()
        );
        exportOptions.addOption(Option.builder("o")
                .longOpt("output")
                .argName("CSV-file")
                .hasArg()
                .desc("CSV file to save the rapla lessons into")
                .build()
        );
    }

    private static void parseCommandLineOptions(String[] arguments) throws ParseException, IOException
    {
        CommandLine additionalCL = new DefaultParser().parse(additionalOptions, arguments, true);

        if (additionalCL.getOptions().length > 0) // additional options (help, version) are given
        {
            if (additionalCL.hasOption("h")) {
                HelpFormatter helpFormatter = new HelpFormatter();
                // collect all options to display help
                Options allOptions = additionalOptions;
                exportOptions.getOptions().forEach(allOptions::addOption);

                helpFormatter.printHelp("rapla2csv", allOptions, true); // TODO get from package name
            } else if (additionalCL.hasOption("v")) {
                System.out.println("v0.1"); // TODO get from somewhere automatically
            }

            System.exit(0); // end program after additional options
        } else // export options given
        {
            CommandLine exportCL = new DefaultParser().parse(exportOptions, arguments);

            if (exportCL.hasOption("o")) fileName = exportCL.getOptionValue("o");

            // check if types of export options are correct
            try {
                timeFrom = LocalDate.parse(exportCL.getOptionValue("f"));
                timeUntil = LocalDate.parse(exportCL.getOptionValue("u"));
                raplaLink = new URIBuilder(exportCL.getOptionValue("l"));
            } catch (Exception e) {
                throw new ParseException("Options are not in a valid format");
            }
        }
    }
}
