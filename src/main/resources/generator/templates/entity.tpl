package #(entityPkg);

import com.mugsun.core.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;
#for(imp : imports)
import #(imp);
#end

/**
 * #(functionName)
 *
 * @author #(author)
 */
@Table("#(tableName)")
public class #(entityName) extends BaseEntity {
#for(col : columns)

	/** #(col.comment) */
	private #(col.javaType) #(col.javaField);
#end
#for(col : columns)

	public #(col.javaType) get#(col.capField)() {
		return #(col.javaField);
	}

	public void set#(col.capField)(#(col.javaType) #(col.javaField)) {
		this.#(col.javaField) = #(col.javaField);
	}
#end
}
