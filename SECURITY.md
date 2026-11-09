# Security Policy

## Supported Versions

Only the latest release receives security fixes. Run the current version.

## Reporting a Vulnerability

Please report security issues privately to hnacho@proton.me, not as public
GitHub issues. Include:

- The affected version.
- Steps to reproduce or a proof of concept.
- The impact you see (what an attacker could do).

You can expect an acknowledgement within a few days. If the issue is
confirmed, the fix ships in the next release and you get credit in the
changelog unless you prefer otherwise.

## Scope Notes

Gallery is a local Android app. It reads the system MediaStore and keeps its
own metadata in app storage. There is no network backend. Problems in Honor's
original app or in Android's MediaStore belong to their own projects, but if
you are unsure where a problem lives, report it here anyway and it will be
routed.
