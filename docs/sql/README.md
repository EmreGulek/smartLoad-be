# Veritabanı şema script'leri

Tablolar elle oluşturulur; Hibernate `ddl-auto=validate` ile sadece uyumu kontrol eder.

## Bağlantı (varsayılan)

| Alan | Değer |
|------|--------|
| Host | `localhost:5432` |
| Database | `smartload` |
| User | `smartload` |
| Password | `smartload_dev` |

## Çalıştırma

Sırayla:

```bash
psql -h localhost -p 5432 -U smartload -d smartload -f docs/sql/001-users.sql
psql -h localhost -p 5432 -U smartload -d smartload -f docs/sql/002-manifests.sql
```

Kontrol:

```bash
psql -h localhost -p 5432 -U smartload -d smartload -c "\dt"
```

Ardından backend'i başlatın (`./mvnw spring-boot:run`). Startup başarılıysa şema entity'lerle uyumludur.

## Dosyalar

| Dosya | Tablo | Entity |
|-------|--------|--------|
| `001-users.sql` | `users` | `User.java` |
| `002-manifests.sql` | `manifests` | `Manifest.java` |
| `003-shipments.sql` | `shipments` | `Shipment.java` |
| `004-packages.sql` | `packages` | `Package.java` |
| `005-audit-log.sql` | `audit_log` | `AuditLog.java` |

Yeni tablo eklerken bir sonraki numaralı `.sql` dosyası ekleyin ve ilgili `@Entity` ile eşleştirin.

### Manifest source layer

- `source_grid` — immutable Excel grid (`headers` + data `rows` only).
- `column_mapping` — frozen field→column index map from import.
- `validation_result` — validate-time audit snapshot; `packages` is the editable working set.
- Round-trip: `packages.source_row_number` = Excel row; `source_grid.rows[source_row_number - 2]`.
