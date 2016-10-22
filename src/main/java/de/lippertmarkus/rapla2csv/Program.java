package de.lippertmarkus.rapla2csv;

import org.apache.commons.cli.*;

import java.net.URL;
import java.time.LocalDate;

/**
 * Entry class for the application.
 * Parses command line options, extract lessons from rapla and export to CSV.
 */
public class Program
{
    /**
     * The reader object to extract the data from rapla
     */
    private RaplaReader raplaReader;

    /**
     * Command line options for the extraction range and output file etc.
     */
    private Options exportOptions = new Options();

    /**
     * Command line options like help, version
     */
    private Options additionalOptions = new Options();

    /**
     * Begin of the extraction range for the lessons
     */
    private LocalDate timeFrom;

    /**
     * End of the extraction range for the lessons
     */
    private LocalDate timeUntil;

    /**
     * Link to the rapla web calendar view with a key or a user and file
     */
    private URL raplaLink;

    /**
     * File name of the CSV file to export
     */
    private String exportFileName = "rapla.csv";


    /**
     * Constructs a new instance of the application and parses command line options
     *
     * @param args command line arguments
     */
    private Program(String[] args)
    {
        createCommandLineOptions();

        try {
            parseCommandLineOptions(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Static main method of the application
     *
     * @param args command line arguments
     */
    public static void main(String[] args)
    {
        new Program(args).extractAndExportLessonsToCSV();
    }

    /**
     * Adds several command line options for parsing them later
     */
    private void createCommandLineOptions()
    {
        createAdditionalCommandLineOptions();
        createExportCommandLineOptions();
    }

    /**
     * Adds additional command line options like 'help' and 'version'
     */
    private void createAdditionalCommandLineOptions()
    {
        additionalOptions.addOption("h", "help", false, "Shows this help");
        additionalOptions.addOption("v", "version", false, "Show version number");
    }

    /**
     * Adds export command line options for setting date range, link and output file
     */
    private void createExportCommandLineOptions()
    {
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

    /**
     * Parses all given command line options. First the additional options, if none, the export options
     *
     * @param arguments command line arguments
     * @throws ParseException when command line arguments couldn't be parsed
     */
    private void parseCommandLineOptions(String[] arguments) throws ParseException
    {
        CommandLine additionalCL = new DefaultParser().parse(additionalOptions, arguments, true);

        if (additionalCL.getOptions().length > 0) {
            parseAdditionalCommandLineOptions(additionalCL);
        } else {
            parseExportCommandLineOptions(arguments);
        }
    }

    /**
     * Parses the additional command line options and outputs help text or version
     *
     * @param additionalCL command line object for additional options
     */
    private void parseAdditionalCommandLineOptions(CommandLine additionalCL)
    {
        Package currPackage = this.getClass().getPackage(); // get package for extracting information

        if (additionalCL.hasOption("h")) {
            HelpFormatter helpFormatter = new HelpFormatter();

            // collect all options to display help
            Options allOptions = additionalOptions;
            exportOptions.getOptions().forEach(allOptions::addOption);

            helpFormatter.printHelp(currPackage.getImplementationTitle(), allOptions, true);
        } else if (additionalCL.hasOption("v")) {
            System.out.println(currPackage.getImplementationTitle() + " " + currPackage.getSpecificationVersion());
            System.out.println("(c) " + currPackage.getImplementationVendor());
        }

        System.exit(0); // end program after additional options
    }

    /**
     * Parses the export command line options and sets date range, rapla link and output file for ectraction
     *
     * @param arguments command line arguments
     * @throws ParseException when export options couldn't be parsed
     */
    private void parseExportCommandLineOptions(String[] arguments) throws ParseException
    {
        CommandLine exportCL = new DefaultParser().parse(exportOptions, arguments);

        if (exportCL.hasOption("o"))
            exportFileName = exportCL.getOptionValue("o");

        // check if types of export options are correct
        try {
            timeFrom = LocalDate.parse(exportCL.getOptionValue("f"));
            timeUntil = LocalDate.parse(exportCL.getOptionValue("u"));
            raplaLink = new URL(exportCL.getOptionValue("l"));
        } catch (Exception e) {
            throw new ParseException("Options are not in a valid format");
        }
    }

    /**
     * Extracts the lessons from rapla with the parsed command line options and export them to CSV file
     */
    private void extractAndExportLessonsToCSV()
    {
        extractLessons();
        exportLessonsToCSV();
    }

    /**
     * Extracts the lessons from rapla and outputs statistics afterward
     */
    private void extractLessons()
    {
        try {
            raplaReader = new RaplaReader(timeFrom, timeUntil, raplaLink);
            raplaReader.getLessonsFromRapla();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println(raplaReader.getExtractedLessonsInfo());
    }

    /**
     * Exports the extracted lessons to a CSV file for import into calendar software
     */
    private void exportLessonsToCSV()
    {
        try {
            raplaReader.exportToCSV(exportFileName);
            System.out.println("Export done: " + exportFileName);
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }
}
