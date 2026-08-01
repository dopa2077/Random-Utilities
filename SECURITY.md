# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| 0.0.x   | Limited            |
| < 0.0.3 | :x:                |

Only the latest release receives security fixes. Older versions are not supported.

## Reporting a Vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Instead, use one of these methods:

1. **Preferred**: [Open a private security advisory](https://github.com/dopa2077/Random-Utilities/security/advisories/new)  
   (Go to the Security tab → “Report a vulnerability”)

2. Contact the maintainer privately through GitHub (e.g. via a private message if available, or by opening a normal issue *without* sharing exploit details and asking for a private channel).

### What to include in your report

- Description of the vulnerability
- Affected version(s) / commit
- Steps to reproduce
- Potential impact (especially anything that could lead to remote code execution, arbitrary file access, or server-side abuse)
- Proof-of-concept if possible (keep it minimal)

### Response expectations

- You should receive an acknowledgement within a few days.
- I will investigate and aim to release a fix as quickly as reasonable for a solo project.
- Please allow time for coordinated disclosure before publishing details publicly.

## Scope

This is a small utility mod. Security-relevant issues typically include:

- Remote code execution or arbitrary code loading
- Path traversal / arbitrary file read/write
- Unsafe handling of network data, configs, or resource packs
- Privilege escalation or server-side exploits that go beyond normal Minecraft gameplay

Regular bugs, crashes, or feature requests should go in the normal Issues tab.

## Additional notes

- Always download the mod only from the official GitHub Releases, or from trusted platforms (CurseForge / Modrinth) once published there.
- The mod does not intentionally collect telemetry or personal data.
- Like any Minecraft mod, it runs inside the game’s JVM, treat third-party `.jar` files with appropriate caution.

Thank you for helping keep the mod (and players’ worlds/servers) safer.
