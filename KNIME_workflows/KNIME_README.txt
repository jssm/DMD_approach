KNIME workflow notes:

For workflow dependencies, please see the KNIME_dependencies.txt file.

  - the data processing done for each Task had to be done separately, so there is a workflow for each task;
  - unfortunately, due to the "traffic light" system KNIME's nodes' use, it is not possible to simply run these workflows in one go (some nodes' input is only available after the previous nodes run, and they consider that they are not configured, which stops the flow); thus it is advised to run the workflow in phases (basically, as far as it can go);
  - please do not forget to change the paths of the input files on Reader nodes (orange coloured) and of the output files on Create File Name nodes (brown coloured); 
  - the parts of the workflows for the occurrences files (with the counts and frequencies) were not being use at the end, so they might be outdated (optional and does not interfere with the rest);
  - the input data files are not provided since the dataset belongs to the consortium and it is not public.