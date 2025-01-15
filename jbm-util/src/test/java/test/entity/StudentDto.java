package test.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class StudentDto extends Student {

    public String getTestName() {
        return this.getName();
    }
}
