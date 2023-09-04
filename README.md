## Eclipse Liberty Project Helper &mdash; `elph`
Use this tool to help set up an eclipse workspace for working on Open-Liberty.

## Prereqs
- Java 17 or above
- [SDKMAN](https://sdkman.io/) (optional, but recommended for managing Java SDKs)

## Installing and running the tool
- Clone this git repository locally.
- Add the newly created directory to your PATH.
- Invoke `elph config -i` &mdash; this will build the tool and then configure it interactively. Run it again without the `-i` to display (and validate) the config.
- Invoke `elph list '*yoko*'` &mdash; this will list all the known projects that contain 'yoko' in the title.
- Invoke `elph start-eclipse` &mdash; this will start Eclipse with the configured workspace.
