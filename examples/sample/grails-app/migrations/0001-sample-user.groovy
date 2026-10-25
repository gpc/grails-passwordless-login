databaseChangeLog = {
    changeSet(author: 'sample', id: '0001-sample-user') {
        createTable(tableName: 'sample_user') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) { constraints(primaryKey: true, primaryKeyName: 'sample_user_pk') }
            column(name: 'version', type: 'BIGINT') { constraints(nullable: false) }
            column(name: 'email', type: 'VARCHAR(254)') { constraints(nullable: false, unique: true, uniqueConstraintName: 'sample_user_email_uk') }
            column(name: 'display_name', type: 'VARCHAR(200)') { constraints(nullable: false) }
            column(name: 'enabled', type: 'BOOLEAN') { constraints(nullable: false) }
            column(name: 'admin', type: 'BOOLEAN') { constraints(nullable: false) }
        }
    }
}
