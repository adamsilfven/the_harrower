# Harrower compatibility audit

Minecraft Java 1.21.1, NeoForge 21.1.259, Java 21.

Harrower is standalone with optional Leyline Convergence Core integration. Core must not be made a hard dependency by accident. The original integration is guarded to avoid class loading failures when Core is absent.

Existing IDs and resource paths must remain stable for saved-world compatibility. The Harrower entity is registered under lc_harrower. Keep server-authoritative combat and damage separate from client presentation.

This historical audit predates the final 1.0.0 release. See the release validation and source metadata for the current verified compatibility boundary.
