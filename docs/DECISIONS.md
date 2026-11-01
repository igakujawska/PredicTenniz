# Engineering decisions

## ADR-001: a modular monolith before microservices

Use Spring Boot for business logic, persistence and provider integration. A single backend is easier to debug, test and run during interviews. The Python research workflow is offline; a Python model service is justified only when a trained model needs dependencies unavailable in Java.

## ADR-002: an interpretable baseline before deep learning

Elo is auditable and immediately exercises the whole application. ML must beat Elo on a chronological holdout using log loss and Brier score. Neural networks are an experiment, not a marketing label or a promise of better accuracy.

## ADR-003: exact propagation before Monte Carlo

For a fixed knockout draw with independent match outcomes conditional on player identity, dynamic programming gives exact advancement probabilities. This removes random seeds and sampling noise. Monte Carlo becomes useful when introducing uncertain injuries, rating distributions, correlated form or more complex tournament rules.

## ADR-004: prize money is an expectation

Each prize is the **total payout for the finishing position**, not an incremental payment. Expected gross prize is the sum of each exit probability times its total payout. Taxes, sponsorship, costs, appearance fees and currency conversions are outside v1.

## ADR-005: portable SQL and local-first development

H2 removes setup friction; PostgreSQL is available through a Spring profile. Flyway migrations are shared. SQL persistence uses JDBC and explicit queries to make database behavior visible to a learner. Public deployment needs authentication, rate limiting, a secrets manager, a managed database and a separate threat review.
