# Compose Rules Documentation

This directory contains the source documentation for Compose Rules, which is built
using [MkDocs](https://www.mkdocs.org/) with the [Material theme](https://squidfunk.github.io/mkdocs-material/).

## Documentation Structure

- `index.md` - Overview and introduction
- `ktlint.md` - Guide for using Compose Rules with ktlint
- `detekt.md` - Guide for using Compose Rules with detekt
- `rules.md` - Comprehensive list of all rules with examples

## Versioning

The documentation is **versioned** using [mike](https://github.com/jimporter/mike):

- **Stable versions** (e.g., `0.5.0`, `0.4.0`) - Created automatically when a release tag is pushed
- **Development version** (`next`) - Updated automatically on every push to `main` branch
- **Default version** (`latest`) - Always points to the most recent stable release

### Viewing Different Versions

Visit the [documentation site](https://mrmans0n.github.io/compose-rules/) and use the version selector in the header to
switch between:

- Latest stable version (default)
- Specific release versions
- Next (development) version

## Local Development

### Quick Preview (Recommended)

For fast iteration when writing documentation:

```bash
# Install dependencies (first time only)
uv sync

# Serve the docs locally
uv run mkdocs serve
```

Visit `http://localhost:8000` to see your changes live. This serves the current docs without versioning.

### Preview with Versioning

To see how the documentation will look with the version selector:

```bash
# Deploy a test version locally (won't push to GitHub)
uv run mike deploy dev --no-push

# Serve with version selector
uv run mike serve
```

Visit `http://localhost:8000` to see the versioned site.

## Making Changes

1. Edit the relevant `.md` files in this directory
2. Preview your changes locally with `mkdocs serve`
3. Commit and push to a feature branch
4. Create a pull request

When your PR is merged to `main`:

- The `next` version of the docs will be automatically updated
- Changes will be visible at `https://mrmans0n.github.io/compose-rules/next/`

When a new release is created:

- A new versioned documentation site is created automatically
- The `latest` version is updated to point to the new release

## Documentation Guidelines

- Use clear, concise language
- Include code examples where relevant
- Link to related rules or sections when applicable
- Test all code examples to ensure they work
- Follow the existing documentation structure and style

## Useful Commands

```bash
# Serve docs locally (fast, no versioning)
uv run mkdocs serve

# Build the docs (output in site/ directory)
uv run mkdocs build

# List all deployed versions
uv run mike list

# Preview versioned docs locally
uv run mike serve
```

## Resources

- [MkDocs Documentation](https://www.mkdocs.org/)
- [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)
- [mike Documentation](https://github.com/jimporter/mike)
- [Markdown Guide](https://www.markdownguide.org/)

## Questions?

If you have questions about the documentation:

- Open an issue on GitHub
- Reach out to the maintainers
