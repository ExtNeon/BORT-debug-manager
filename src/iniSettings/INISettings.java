package iniSettings;

import iniSettings.exceptions.AlreadyExistsException;
import iniSettings.exceptions.IniSettingsException;
import iniSettings.exceptions.NotFoundException;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Класс, позволяющий представить INI - файл, как объект.
 * Имеются следующие возможности:
 * <ul>
 *     <li>Добавление новых секций с записями
 *     <li>Обновление уже существующих секций</li>
 *     <li>Поиск секции по имени</li>
 *     <li>Импорт секций из файла, и их сохранение в файл</li>
 * </ul>
 * @author Малякин Кирилл. 15ИТ20.
 */
public class INISettings {
    private ArrayList<INISettingsSection> sections = new ArrayList<>();

    /**
     * Метод добавляет секцию @code{newSection} в список секций.
     *
     * @param newSection Секция, которую необходимо добавить.
     * @throws AlreadyExistsException Если в списке секций уже имеется секция с таким же именем.
     */
    public void addSection(INISettingsSection newSection) throws AlreadyExistsException {
        for (INISettingsSection selectedSection : sections) {
            if (selectedSection.getSectionName().equals(newSection.getSectionName())) {
                throw new AlreadyExistsException();
            }
        }
        sections.add(newSection);
    }

// --Commented out by Inspection START (01.07.2018 0:18):
//    /**
//     * Метод обновляет секцию в списке, если её имя совпадает с именем секции @code{section}.
//     * В противном случае, секция просто добавляется в список.
//     * @param section Секция, которую необходимо обновить.
//     */
//    public void updateSection(INISettingsSection section) {
//        try {
//            sections.set(sections.indexOf(getSectionByName(section.getSectionName())), section);
//        } catch (NotFoundException e) {
//            sections.add(section);
//        }
//    }
// --Commented out by Inspection STOP (01.07.2018 0:18)

    /**
     * Возвращает секцию, найденную по её имени.
     *
     * @param sectionName Имя секции, которую необходимо найти
     * @return Секция с заданным именем.
     * @throws NotFoundException В случае, если секции с подобным именем нет в списке.
     */
    public INISettingsSection getSectionByName(String sectionName) throws NotFoundException {
        for (INISettingsSection selectedSection : sections) {
            if (selectedSection.getSectionName().equals(sectionName)) {
                return selectedSection;
            }
        }
        throw new NotFoundException();
    }

//    /**
//     * @return Список секций целиком.
//     */
//    public ArrayList<INISettingsSection> getSections() {
//        return sections;
//    }

    /**
     * Метод экспортирует список секций в файл @code{filename} в соответствие с форматом INI. Файл перезаписывается.
     * @param fileName Файл, в который необходимо экспортировать список секций.
     * @throws IOException В случае проблем с доступом к файлу и общих ошибок ввода - вывода.
     */
    public void saveToFile(String fileName) throws IOException {
        try (BufferedWriter textFile = new BufferedWriter(new FileWriter(fileName))) {
            String iniFileText = this.toString();
            textFile.write(iniFileText);
            textFile.flush();
        }
    }

    /**
     * Метод импортирует список секций из файла @code{fileName}. Импортированный список замещает тот, что был до него.
     * @param fileName Файл, из которого требуется импортировать секции
     * @throws IOException В случае проблем с записью или ошибок ввода - вывода.
     */
    public void loadFromFile(String fileName) throws IOException, IniSettingsException {
        try (BufferedReader textFile = new BufferedReader(new FileReader(fileName))) {
            String readedText = textFile.lines().collect(Collectors.joining("\n"));
            importFromText(readedText);
        }
    }

    /**
     * Метод импортирует список секций из текста, оформленного согласно формату INI. Импортированный список замещает тот, что был до него.
     * @param text Текст, из которого требуется импортировать секции
     */
    // TODO: 27.03.2018 Сделай его менее ужасным
    private void importFromText(String text) throws IniSettingsException {
        ArrayList<String> lines = splitTextIntoLines(text);
        sections = new ArrayList<>();
        while (lines.size() > 0) {
            int startIndex = -1, lastIndex = lines.size();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("[")) {
                    if (startIndex > 0) {
                        lastIndex = i;
                        break;
                    } else {
                        startIndex = i;
                    }
                }
            }
            if (startIndex < 0) {
                break;
            }
            sections.add(new INISettingsSection(lines.get(startIndex).substring(1, lines.get(startIndex).length() - 1).trim()));
            for (int i = startIndex + 1; i < lastIndex; i++) {
                if (lines.get(i).contains("=")) {
                    sections.get(sections.size() - 1).addField(new INISettingsRecord(lines.get(i)));
                }
            }
            for (int i = startIndex; i < lastIndex; i++) {
                lines.remove(startIndex);
            }
        }
    }

    /**
     * Разбивает строку на подстроки по признаку переноса строки, и помещает все подстроки в массив для более удобного доступа.
     * @param text Текст, который необходимо разбить на подстроки
     * @return Массив со строками, представляющими собой отдельные подстроки, разделённые по символу переноса строки.
     */
    private ArrayList<String> splitTextIntoLines(String text) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder builder = new StringBuilder(text);
        while (builder.indexOf("\n") != -1) {
            lines.add(builder.substring(0, builder.indexOf("\n")));
            builder.delete(0, builder.indexOf("\n") + 1);
        }
        lines.add(builder.toString());
        return lines;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (INISettingsSection currentSection : sections) {
            builder.append(currentSection);
        }
        return builder.toString();
    }
}
