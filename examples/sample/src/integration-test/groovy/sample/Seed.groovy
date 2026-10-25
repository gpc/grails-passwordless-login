package sample

/**
 * Committed fixtures for integration specs. Every address is unique, so specs never depend on each other's rows.
 */
class Seed {

    static String uid() {
        UUID.randomUUID().toString().take(8)
    }

    static Long user(String displayName, boolean enabled = true, boolean admin = false) {
        SampleUser.withNewTransaction {
            new SampleUser(displayName: displayName, enabled: enabled, admin: admin,
                    email: "${displayName.toLowerCase().replaceAll(/[^a-z]+/, '-')}-${uid()}@example.org")
                    .save(failOnError: true, flush: true).id
        }
    }

    static String emailOf(Long userId) {
        SampleUser.withNewTransaction { SampleUser.get(userId).email }
    }

    static void disable(Long userId) {
        SampleUser.withNewTransaction { SampleUser.get(userId).tap { enabled = false }.save(failOnError: true, flush: true) }
    }
}
