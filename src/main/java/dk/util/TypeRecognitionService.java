package dk.util;

import java.util.Arrays;
import java.util.Map;

/**
 * Сервисный класс для распознавания типов объектов
 */
public class TypeRecognitionService {

    /**
     * Является ли объект картой(построен на базе интерфейса Map)?
     * @param obj проверяемый объект
     * @return true/false
     */
    boolean isMap(Object obj) {
        if (obj != null)
            return Arrays.stream(obj.getClass().getInterfaces()).anyMatch(e -> e.equals(Map.class));
                else return false;
    }

    /**
     * Является ли объект строкой?
     * @param obj проверяемый объект
     * @return true/false
     */
    boolean isString(Object obj) {
        if (obj != null)
            return obj.getClass().equals(String.class);
                else return false;
    }

}
