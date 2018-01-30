# rapla2csv v1.0
Little program to export appointments from rapla (web calendar) to CSV file, which can then be imported into calendars (like Google, etc).
Programmed because some rapla calendars (especially the DHBW ones) don't have the option for rapla's integrated iCal support enabled.

## Usage
Download rapla2csv JAR, switch to download directory, open command line and use `java -jar rapla2csv.jar -h` so see all command line options:

    usage: rapla2csv -f <date> -u <date> -l <link> [-p <proxy string>] [-o <CSV-file>] [-h] [-v]
     -f,--from <date>           Begin of the export time period, e.g. 2015-12-31
     -h,--help                  Shows this help
     -l,--link <link>           Rapla link IN QUOTES, e.g.
                                "http://example.com/rapla?key=abc123"
     -o,--output <CSV-file>     CSV file to save the rapla lessons into
     -p,--proxy <proxy string>  Your proxy settings in format host:port, e.g.
                                myHost:1234
     -u,--until <date>          End of the export time period, e.g. 2016-12-31
     -v,--version               Show version number
