# Update translations from Transifex

## Preparations

1. Install the Transifex client `tx` as described at https://docs.transifex.com/client/installing-the-client

2. You need an account at https://www.transifex.com/signin/ (login with Github, Google or LinkedIn is supported).

3. Create an API token at https://www.transifex.com/user/settings/api/ and remember it for the next step.

4. Create a file `~/.transifexrc` as described at https://docs.transifex.com/client/client-configuration#section-~-transifexrc .
Use `api` as username in this file and your API token from the previous step as password.

## Workflow

Each time you now want to download new translations from https://transifex.com , run this command in the project directory:
```
./gradlew transifexDownload
```
