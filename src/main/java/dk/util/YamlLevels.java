package dk.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для учета уровней yaml-файла.
 * <br><br>
 * Подразумевается, что пользователь в конфигурационном файле будет прописывать цели(уровни) yaml-файла, которые он желает
 * обработать, в таком виде:<br>
 * arrestsmodule/google<br>
 * g1cfrntapb05/SMECredit/configuration<br>
 * В процессе работы рекурсивных вызовов по дереву yaml-файла этот класс облегчит контроль текущего уровня и последующее его
 * сравнение с целями, указанными пользоваталем.
 */
public class YamlLevels {

    private List<String> levels = new ArrayList<>();

    /**
     * Добавить уровень
      * @param level Имя очередного ключа yaml-файла
     */
    public void add(String level) {
        levels.add(level);
    }

    /**
     * Удалить последний уровень
     */
    public void remove() {
        if (levels.size() > 0) levels.remove(levels.size() - 1);
    }

    /**
     * Сформировать полный путь/цель/уровень yaml-файла, который сейчас учтен в этом классе
     * @return полное название уровня
     */
    public String getPath() {
        String path = "";
        if (levels.size() > 0) {
            path = levels.get(0);
            for (int i = 1; i <= levels.size() - 1; i++) {
                path = path + "/" + levels.get(i);
            }
        }
        return path;
    }

}
