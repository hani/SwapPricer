All tests use the Nov 4th dataset euro pack (available for download as part of the R&D info pack)

The code includes the curve data and holiday calendars, so those don't need to be downloaded.

What is needed though is a rate mongodb repository. Download and install mongodb, then when you open the project in IDEA
you should see a run configuration for RateLoader. You'll need to download the 3 rates files from the research folder in the library,
then run the RateLoader with these as input files (in the program parameters, run them one at a time).

Once that's done then you can run all the tests to see the pricer in action