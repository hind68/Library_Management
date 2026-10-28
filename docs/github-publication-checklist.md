# GitHub Publication Checklist

## Mandatory security action

The existing local Git history contains a previously committed MySQL password. Removing it from the current file does not remove it from earlier commits.

Before publishing:

1. Rotate the exposed MySQL password in WAMP/MySQL.
2. Update only the ignored `database-local.properties` file.
3. Confirm `git check-ignore database-local.properties` succeeds.
4. Search the files selected for publication for credentials and private data.

## Clean-history strategy

Do not push the current branch or its existing history to a public repository. Preserve this repository as the working archive, then publish a clean snapshot using one of these approaches:

- Preferred: copy the reviewed working tree, excluding `.git`, ignored files and build output, into a new directory; initialize a new repository and create a fresh initial commit.
- Alternative after explicit approval: create an orphan publication branch containing only the reviewed snapshot and no parent commits.

Neither operation is performed automatically because both change repository history or create a separate repository.

## Files to exclude

- `database-local.properties`
- `.env` and `.env.*`
- `target/`, compiled classes and IDE output
- personal IDE settings
- generated exports that contain real user or library data

## Final checks

```bash
git status --short
git check-ignore database-local.properties
mvn clean test
mvn package
```

Add only real screenshots captured from the running application. Review each image for personal data, credentials and local filesystem paths before committing it.
