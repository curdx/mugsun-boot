package #(controllerPkg);

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import #(entityPkg).#(entityName);
import #(mapperPkg).#(mapperName);
import com.mugsun.core.tool.api.R;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * #(functionName)管理
 *
 * @author #(author)
 */
@RestController
@RequestMapping("#(path)")
@SaCheckLogin
public class #(entityName)Controller {

	private final #(mapperName) #(mapperVar);

	public #(entityName)Controller(#(mapperName) #(mapperVar)) {
		this.#(mapperVar) = #(mapperVar);
	}

	@GetMapping("/page")
	public R<Page<#(entityName)>> page(@RequestParam(defaultValue = "1") long pageNum,
			@RequestParam(defaultValue = "10") long pageSize) {
		return R.data(#(mapperVar).paginate(pageNum, pageSize, QueryWrapper.create().orderBy("id", false)));
	}

	@GetMapping("/detail")
	public R<#(entityName)> detail(@RequestParam Long id) {
		return R.data(#(mapperVar).selectOneById(id));
	}

	@SaCheckPermission("#(permPrefix):save")
	@PostMapping("/submit")
	public R<Void> submit(@RequestBody #(entityName) #(entityVar)) {
		if (#(entityVar).getId() == null) {
			#(mapperVar).insertSelective(#(entityVar));
		} else {
			#(mapperVar).update(#(entityVar));
		}
		return R.success("操作成功");
	}

	@SaCheckPermission("#(permPrefix):remove")
	@PostMapping("/remove")
	public R<Void> remove(@RequestBody List<Long> ids) {
		#(mapperVar).deleteBatchByIds(ids);
		return R.success("删除成功");
	}
}
