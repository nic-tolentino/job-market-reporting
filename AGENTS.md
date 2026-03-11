Read any files and stashed changes, but never execute 'git commit' or 'git push' without my explicit approval in a separate terminal.

When reviewing code, first read from the files that are staged in git, then expand your search within the project if necessary to build the appropriate context.

When writing code, always write tests for the code you write. Focus on writing test for code that is brittle and can't be validating using language, API, lint, or compile time checks (eg, strings, regex, etc).

If you come across a bug, in addition to addressing the bug, make sure you add test coverage which would detect the bug, and also consider how other parts of the codebase coul be impacted by this class of bug and how we can either add unit test coverage to detect it going forward, or refactor the code to make the class of bug impossible.

When writing code, always write documentation for the code you write, to help future contributors understand why something was build the way it was.