package classes;

/**
 * Контейнер с двумя полями и конструктором. Примитив, в котором даже инкапсуляция не реализована.
 */
public class Primitive_KeyValueRecord {
    public int key = 0;
    public String value = "";

    public Primitive_KeyValueRecord(int key, String value) {
        this.key = key;
        this.value = value;
    }
}
