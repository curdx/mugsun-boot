<!-- #(functionName) 管理页（代码生成产物·树表 TreeUtil + 懒展开） -->
<template>
  <div class="art-full-height">
    <ElCard class="art-table-card">
      <div style="margin-bottom: 12px">
        <ElButton @click="openAdd()" v-ripple>新增</ElButton>
      </div>
      <ElTable
        :data="treeData"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
        v-loading="loading"
      >
#for(col : listColumns)
        <ElTableColumn prop="#(col.prop)" label="#(col.label)" min-width="160" />
#end
        <ElTableColumn label="操作" width="220" fixed="right">
          <template v-slot:default="{ row }">
            <ElButton link type="primary" size="small" @click="openAdd(row)">新增子</ElButton>
            <ElButton link type="primary" size="small" @click="openEdit(row)">编辑</ElButton>
            <ElButton link type="danger" size="small" @click="remove(row)">删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>
      <ElDialog v-model="dialogVisible" :title="currentRow.id ? '编辑' : '新增'" width="520px" align-center>
        <ElForm :model="currentRow" label-width="100px">
#for(col : formColumns)
          <ElFormItem label="#(col.label)">
            #(col.control)
          </ElFormItem>
#end
        </ElForm>
        <template v-slot:footer>
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="submit">提交</ElButton>
        </template>
      </ElDialog>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { #(elImports) } from 'element-plus'
#if(dictCodes.size() > 0)
  import { useDict } from '@/hooks/core/useDict'
#end
  import { fetch#(entityName)Tree, fetchSave#(entityName), fetchRemove#(entityName) } from '@/api/#(module)/#(businessKebab)'

  defineOptions({ name: '#(entityName)' })
#if(dictCodes.size() > 0)

  const { #(dictVars) } = useDict(#(dictArgs))
#end

  const treeData = ref<any[]>([])
  const loading = ref(false)
  const dialogVisible = ref(false)
  const currentRow = ref<any>({})

  const loadTree = async (): Promise<void> => {
    loading.value = true
    try {
      treeData.value = ((await fetch#(entityName)Tree()) as any[]) || []
    } finally {
      loading.value = false
    }
  }

  const openAdd = (parent?: any): void => {
    currentRow.value = { parentId: parent ? parent.id : 0 }
    dialogVisible.value = true
  }

  const openEdit = (row: any): void => {
    currentRow.value = { ...row, children: undefined }
    dialogVisible.value = true
  }

  const submit = async (): Promise<void> => {
    await fetchSave#(entityName)(currentRow.value)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    await loadTree()
  }

  const remove = async (row: any): Promise<void> => {
    await ElMessageBox.confirm('确认删除该节点？', '提示', { type: 'warning' })
    await fetchRemove#(entityName)([row.id])
    ElMessage.success('删除成功')
    await loadTree()
  }

  onMounted(loadTree)
</script>
