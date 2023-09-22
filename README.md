## Eclipse Liberty Project Helper &mdash; `elph`
#### A tool to help set up an eclipse workspace for working on Open-Liberty.

#### Post any questions to `#was-elph` on slack

## Prerequisites
- Build Open Liberty locally
- Java 17 or above
- [SDKMAN](https://sdkman.io/) (optional, but recommended for managing Java SDKs)
    - Search for the lastest Java versions to install
      
            sdk list java
    -  Install the latest Semeru version of Java 17 e.g. 17.0.8-sem:
  
            sdk install java 17.x.x-sem
    - Use the Java 17 version you have just installed
  
            sdk use java 17.x.x-sem

## Installing ELPH
- Clone the repo locally
        
        git clone git@github.com:joe-chacko/elph.git
- Add the path to your cloned repo of ELPH to your PATH environment variable so that your command line knows where to find the executable.

  - In an editor of your choice add the following line to your `~/.bashrc` or `~/.zshrc` depending on if you are using bash or z shell:

            export PATH=<path-to-elph-dir>:$PATH
  - Run the following command to immediately apply the changes to your PATH environment variable:

    - Bash Shell

            source ~/.bashrc

    - Z Shell

            source ~/.zshrc

- Try running elph to see if it builds properly

        elph help


## Running ELPH
- Invoke `elph help` &mdash; this will build the tool and give a description of each of ELPH's commands.
- Invoke `elph setup -i` &mdash; this will configure it interactively. Run it again without the `-i` to display (and validate) the config.
- Invoke `elph analyze` &mdash; this will take a little while (under 1 minute) to query bnd about your workspace. 
- Invoke `elph list '*yoko*'` &mdash; this will list all the known projects that contain 'yoko' in the title.
- Invoke `elph eclipse` &mdash; this will start Eclipse with the configured workspace.

## Setting up Eclipse

#### Turn on build automatically
- Click the `Project` tab at the top and ensure `Build Automatically` is checked

#### Install bnd tools
- Install bnd tools by going to the tab at the top of Eclipse and selecting `Help > Eclipse Marketplace > BndTools > Install > Confirm`

#### Filter errors
It will help when debugging Eclipse errors is to filter what you can see in the `Markers` view pane.
- Click on the filter (a funnel icon) near the bottom of the page
    1. Create a new filter called `All Errors` with Scope `No filter, show all elements` and `Show severities` where severity is `Error`
    1. Create a new filter called `Errors on Project` with Scope `On elements in selected projects` and `Show severities` where severity is `Error`

Use `All Errors` when importing projects and just `Errors on Project` when fixing individual problems.

## Importing projects
Below are some useful pointers for importing projects with ELPH. As an example, try the following instructions to import the project `cnf`:
- Run `elph import cnf` to import the project. This should open a window in eclipse.
    - Uncheck all checkboxes in the Import dialog
    - Check the "Hide already open projects" checkbox
- **TIP**: For fast importing, hold down the return button instead of clicking finish each time in Eclipse for each project.
- Go back to your terminal and press return to continue.
- If for some reason you exit the terminal process before importing all the dependent projects (e.g. using CTRL+C), run `elph reimport` to resume importing.
