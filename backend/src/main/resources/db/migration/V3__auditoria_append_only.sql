CREATE TRIGGER impedir_actualizar_auditoria
BEFORE UPDATE ON auditoria
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoría no se puede modificar';

CREATE TRIGGER impedir_borrar_auditoria
BEFORE DELETE ON auditoria
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoría no se puede borrar';
