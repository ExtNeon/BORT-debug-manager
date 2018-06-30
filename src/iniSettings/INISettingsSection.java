package iniSettings;

import iniSettings.exceptions.AlreadyExistsException;
import iniSettings.exceptions.NotFoundException;

import java.util.ArrayList;

/**
 * Класс представляет секцию INI - файла. Имеет собственное имя и список записей.
 * Позволяет добавлять записи и находить их по названию ключа.
 * @author Малякин Кирилл. 15ИТ20.
 */
public class INISettingsSection {
    private String sectionName;
    private final ArrayList<INISettingsRecord> records = new ArrayList<>();

    /**
     * Конструктор. Необходимо лишь указать название данной секции.
     *
     * @param sectionName Название секции
     */
    public INISettingsSection(String sectionName) {
        this.sectionName = sectionName;
    }

    /**
     * @return Имя данной секции
     */
    public String getSectionName() {
        return sectionName;
    }

    /**
     * Метод устанавливает имя секции равным @code(sectionName).
     * @param sectionName Новое имя данной секции.
     */
    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    /**
     * @return Список всех записей
     */
    public ArrayList<INISettingsRecord> getRecords() {
        return records;
    }

    /**
     * Добавляет запись @code{newRecord} в список записей.
     *
     * @param newRecord Запись, которую необходимо добавить в список записей данной секции.
     * @throws AlreadyExistsException В случае, если запись с таким же ключевым значением уже имеется.
     */
    public void addField(INISettingsRecord newRecord) throws AlreadyExistsException {
        for (INISettingsRecord selectedRecord : records) {
            if (selectedRecord.getKey().equals(newRecord.getKey())) {
                throw new AlreadyExistsException();
            }
        }
        records.add(newRecord);
    }

    /**
     * Возвращает запись, ключевое значение которой равно @code{key}.
     * В случае, если такой записи нет в списке, выбрасывается исключение.
     *
     * @param key Ключ записи, по которой её необходимо найти
     * @return Запись, ключ которой равен запрошенному.
     * @throws NotFoundException В случае, если записи с таким ключевым значением нет в списке.
     */
    public INISettingsRecord getFieldByKey(String key) throws NotFoundException {
        for (INISettingsRecord selectedRecord : records) {
            if (selectedRecord.getKey().equals(key)) {
                return selectedRecord;
            }
        }
        throw new NotFoundException();
    }

    @Override
    public String toString() {
        String result = '[' + sectionName + "]\n";
        for (INISettingsRecord selectedRecord : records) {
            result += selectedRecord.toString() + '\n';
        }
        return result;
    }
}
