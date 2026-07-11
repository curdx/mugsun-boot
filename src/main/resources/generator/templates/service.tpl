package #(servicePkg);

import #(entityPkg).#(entityName);
import #(entityPkg).#(childEntityName);
import #(mapperPkg).#(mapperName);
import #(mapperPkg).#(childMapperName);
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * #(functionName)主子表级联服务。
 *
 * @author #(author)
 */
@Service
public class #(entityName)Service {

	private final #(mapperName) #(mapperVar);
	private final #(childMapperName) #(childMapperVar);

	public #(entityName)Service(#(mapperName) #(mapperVar), #(childMapperName) #(childMapperVar)) {
		this.#(mapperVar) = #(mapperVar);
		this.#(childMapperVar) = #(childMapperVar);
	}

	/** 详情：主表 + 子表列表 */
	public #(entityName) detail(Long id) {
		#(entityName) #(entityVar) = #(mapperVar).selectOneById(id);
		if (#(entityVar) != null) {
			#(entityVar).set#(subListCap)(#(childMapperVar).selectListByQuery(
				QueryWrapper.create().eq("#(subJoinColumn)", id)));
		}
		return #(entityVar);
	}

	/** 级联保存：存主表 → 按主键重建子表（删旧插新） */
	@Transactional(rollbackFor = Exception.class)
	public void save(#(entityName) #(entityVar)) {
		if (#(entityVar).getId() == null) {
			#(mapperVar).insertSelective(#(entityVar));
		} else {
			#(mapperVar).update(#(entityVar));
		}
		Long mainId = #(entityVar).getId();
		#(childMapperVar).deleteByQuery(QueryWrapper.create().eq("#(subJoinColumn)", mainId));
		List<#(childEntityName)> subList = #(entityVar).get#(subListCap)();
		if (subList != null) {
			for (#(childEntityName) sub : subList) {
				sub.setId(null);
				sub.set#(subJoinCap)(mainId);
				#(childMapperVar).insertSelective(sub);
			}
		}
	}

	/** 级联删除：删子表 → 删主表 */
	@Transactional(rollbackFor = Exception.class)
	public void remove(List<Long> ids) {
		if (ids != null && !ids.isEmpty()) {
			#(childMapperVar).deleteByQuery(QueryWrapper.create().in("#(subJoinColumn)", ids));
			#(mapperVar).deleteBatchByIds(ids);
		}
	}
}
