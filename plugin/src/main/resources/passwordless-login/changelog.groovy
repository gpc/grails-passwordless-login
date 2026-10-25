/*
 * The plugin's tables, for an application that keeps its schema in Liquibase (grails-database-migration):
 *
 *     include file: 'passwordless-login/changelog.groovy'
 *
 * login_token is the plugin's own. user_entities and user_credentials are Spring Security's WebAuthn tables, as its
 * PostgreSQL schema has them, plus a unique sign-in name (the email).
 */
databaseChangeLog = {

    changeSet(author: 'passwordless-login', id: 'login-token') {
        createTable(tableName: 'login_token') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) { constraints(primaryKey: true, primaryKeyName: 'login_token_pk') }
            column(name: 'account_id', type: 'VARCHAR(64)') { constraints(nullable: false) }
            // SHA-256 hex of the token. The token itself is never stored.
            column(name: 'token_hash', type: 'CHAR(64)') { constraints(nullable: false, unique: true, uniqueConstraintName: 'login_token_hash_uk') }
            column(name: 'created_at', type: 'TIMESTAMP WITH TIME ZONE') { constraints(nullable: false) }
            column(name: 'expires_at', type: 'TIMESTAMP WITH TIME ZONE') { constraints(nullable: false) }
            column(name: 'used_at', type: 'TIMESTAMP WITH TIME ZONE')
            column(name: 'requested_from', type: 'VARCHAR(45)')
            column(name: 'context', type: 'VARCHAR(100)')
        }
        createIndex(tableName: 'login_token', indexName: 'login_token_account_idx') { column(name: 'account_id') }
        createIndex(tableName: 'login_token', indexName: 'login_token_expires_at_idx') { column(name: 'expires_at') }
    }

    changeSet(author: 'passwordless-login', id: 'passkeys') {
        createTable(tableName: 'user_entities') {
            column(name: 'id', type: 'VARCHAR(1000)') { constraints(primaryKey: true, primaryKeyName: 'user_entities_pk') }
            column(name: 'name', type: 'VARCHAR(100)') { constraints(nullable: false, unique: true, uniqueConstraintName: 'user_entities_name_uk') }
            column(name: 'display_name', type: 'VARCHAR(200)')
        }
        createTable(tableName: 'user_credentials') {
            column(name: 'credential_id', type: 'VARCHAR(1000)') { constraints(primaryKey: true, primaryKeyName: 'user_credentials_pk') }
            column(name: 'user_entity_user_id', type: 'VARCHAR(1000)') {
                constraints(nullable: false, foreignKeyName: 'user_credentials_user_entity_fk', references: 'user_entities(id)')
            }
            column(name: 'public_key', type: 'BYTEA') { constraints(nullable: false) }
            column(name: 'signature_count', type: 'BIGINT')
            column(name: 'uv_initialized', type: 'BOOLEAN')
            column(name: 'backup_eligible', type: 'BOOLEAN') { constraints(nullable: false) }
            column(name: 'authenticator_transports', type: 'VARCHAR(1000)')
            column(name: 'public_key_credential_type', type: 'VARCHAR(100)')
            column(name: 'backup_state', type: 'BOOLEAN') { constraints(nullable: false) }
            column(name: 'attestation_object', type: 'BYTEA')
            column(name: 'attestation_client_data_json', type: 'BYTEA')
            column(name: 'created', type: 'TIMESTAMP')
            column(name: 'last_used', type: 'TIMESTAMP')
            column(name: 'label', type: 'VARCHAR(1000)') { constraints(nullable: false) }
        }
        createIndex(tableName: 'user_credentials', indexName: 'user_credentials_user_entity_idx') { column(name: 'user_entity_user_id') }
    }
}
