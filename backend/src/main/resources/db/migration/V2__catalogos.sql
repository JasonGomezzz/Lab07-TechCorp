INSERT INTO rol (id, codigo, nombre) VALUES
  (1, 'ADMINISTRADOR', 'Administrador'),
  (2, 'GERENTE', 'Gerente'),
  (3, 'SUPERVISOR', 'Supervisor'),
  (4, 'EMPLEADO', 'Empleado'),
  (5, 'AUDITOR', 'Auditor'),
  (6, 'INVITADO', 'Invitado');

INSERT INTO permiso (id, codigo, descripcion) VALUES
  (1, 'CREAR_DOCUMENTO', 'Crear documentos'),
  (2, 'CONSULTAR_DOCUMENTO', 'Consultar documentos'),
  (3, 'MODIFICAR_DOCUMENTO', 'Modificar documentos'),
  (4, 'ELIMINAR_DOCUMENTO', 'Eliminar documentos'),
  (5, 'APROBAR_DOCUMENTO', 'Aprobar documentos'),
  (6, 'VER_AUDITORIA', 'Consultar auditoría'),
  (7, 'GESTIONAR_USUARIOS', 'Gestionar usuarios'),
  (8, 'ASIGNAR_ROLES', 'Asignar roles'),
  (9, 'GESTIONAR_CONFIGURACION', 'Gestionar políticas de autorización');

INSERT INTO rol_permiso (rol_id, permiso_id) VALUES
  (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),
  (2,1),(2,2),(2,3),(2,4),(2,5),(2,6),
  (3,1),(3,2),(3,3),(3,5),
  (4,1),(4,2),(4,3),
  (5,2),(5,6),
  (6,2);

INSERT INTO departamento (id, codigo, nombre) VALUES
  (1, 'TI', 'Tecnología de la Información'),
  (2, 'FINANZAS', 'Finanzas'),
  (3, 'RRHH', 'Recursos Humanos'),
  (4, 'AUDITORIA', 'Auditoría'),
  (5, 'OPERACIONES', 'Operaciones');

INSERT INTO politica (codigo, nombre, descripcion, acciones, roles_exentos, parametros, activa) VALUES
  ('P1_DEPARTAMENTO', 'Departamento', 'El usuario y el documento pertenecen al mismo departamento', '["CREATE","READ","UPDATE","DELETE","APPROVE"]', '[]', '{}', TRUE),
  ('P2_NIVEL_SEGURIDAD', 'Nivel de seguridad', 'El nivel del usuario cubre la confidencialidad del documento', '["CREATE","READ","UPDATE","DELETE","APPROVE"]', '[]', '{}', TRUE),
  ('P3_PROPIEDAD', 'Propiedad', 'Solo el propietario puede modificar, salvo roles exentos', '["UPDATE"]', '["GERENTE","ADMINISTRADOR"]', '{}', TRUE),
  ('P4_HORARIO', 'Horario', 'Documentos confidenciales solo durante horario permitido', '["READ","UPDATE","DELETE","APPROVE"]', '[]', '{"nivelMinimo":4,"inicio":"08:00","fin":"18:00"}', TRUE),
  ('P5A_PAIS_USUARIO', 'País del usuario', 'El país del usuario coincide con el del documento', '["CREATE","READ","UPDATE","DELETE","APPROVE"]', '[]', '{}', TRUE),
  ('P5B_UBICACION', 'Ubicación', 'Documentos de Perú solo desde Perú', '["READ","UPDATE","DELETE","APPROVE"]', '[]', '{"pais":"PERU"}', TRUE),
  ('P6_DISPOSITIVO', 'Dispositivo', 'Documentos confidenciales solo desde dispositivo corporativo', '["READ","UPDATE","DELETE","APPROVE"]', '[]', '{"nivelMinimo":4}', TRUE),
  ('P7_ESTADO_USUARIO', 'Estado del usuario', 'Solo usuarios activos pueden acceder', '["*"]', '[]', '{}', TRUE),
  ('P8_INVITADO', 'Invitados', 'Invitados externos solo consultan documentos publicados de nivel 1 o inferior', '["READ"]', '[]', '{"nivelMaximo":1}', TRUE),
  ('P9_VIGENCIA_INVITADO', 'Vigencia de invitados', 'El acceso temporal del invitado no ha vencido', '["READ"]', '[]', '{}', TRUE);
