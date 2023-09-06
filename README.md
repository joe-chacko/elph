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
- Invoke `elph help` &mdash; this will give a description of each of ELPH's commands.
- Invoke `elph setup -i` &mdash; this will build the tool and then configure it interactively. Run it again without the `-i` to display (and validate) the config.
- Invoke `elph list '*yoko*'` &mdash; this will list all the known projects that contain 'yoko' in the title.
- Invoke `elph eclipse` &mdash; this will start Eclipse with the configured workspace.

## Setting up Eclipse

#### Turn on build automatically
- Click the `Project` tab at the top and ensure `Build Automatically` is clicked

#### Install bnd tools
- Install bnd tools by going to the tap at the top of Eclipse and selecting `Help > Eclipse Marketplace > BndTools > Install > Confirm`

#### Filter errors
1. Click on the filter near the bottom of the page
2. If you are importing projects then choose `All Errors in Project > On elements in selected projects > Apply and Close`
3. If you are ready to develop a project then choose `Errors/Warnings on Project > On elements in selected projects > Apply and Close`

## Import some projects
- Run `elph import cnf` to import the vital first project that bndtools needs!
    - Uncheck all checkboxes in the Import dialog
    - Check the "Hide already open projects" checkbox
- Run `elph import build.image build.sharedResources` to import the next two initial projects.
- Other projects to try importing early on:
    - `com.ibm.ws.ras.instrument`
    - `fattest.simplicity`
