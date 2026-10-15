// 合成记录与时钟，执行真实详情/媒体脚本及模板；不访问业务网络或真实正文。
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict')
const ts=require('typescript'),vue=require('vue'),{parse}=require('vue/compiler-sfc')
const {compile}=require(require.resolve('@vue/compiler-dom',{paths:[path.dirname(require.resolve('vue'))]}))
const root=path.resolve(__dirname,'..'),RecordStatus={DRAFT:'DRAFT',SAVED:'SAVED',SEALED:'SEALED',UNLOCKED:'UNLOCKED'}
const synthetic=()=>({id:30,status:'SEALED',title:'synthetic title',content:'synthetic sealed body must not render',createdAt:'2000-01-01T00:00:00Z',unlockAt:'2000-01-01T01:02:03Z',canReply:false,hasReply:false,attachments:[],agentMemoryExcluded:false,agentMemoryContextNote:'synthetic existing note'})
function setup(file,extra={}) {
 const {descriptor}=parse(fs.readFileSync(path.join(root,'src',file),'utf8'))
 const clock={now:Date.parse('2000-01-01T00:00:00Z')},state={auth:true,preview:false},timers=new Map(),cleanups=[],calls=[],toasts=[],modals=[]
 const store=vue.reactive({detail:synthetic(),fetchDetail:async()=>store.detail})
 let update=async(id,excluded,contextNote)=>({...store.detail,agentMemoryExcluded:excluded,agentMemoryContextNote:contextNote})
 const uni={showToast:x=>toasts.push(x.title),showModal:x=>modals.push(x),navigateTo(){},switchTab(){},navigateBack(){},previewImage(){},createInnerAudioContext:()=>({onEnded(){},onError(){},play(){},stop(){},destroy(){}})}
 const services={recordService:{updateAgentMemoryPolicy:async(...args)=>{calls.push(['memory',...args]);return update(...args)}},dataOwnershipService:{prepareDeletion:async()=>{calls.push(['delete']);return{} }},attachmentService:{createAccessUrl:async(id,attachmentId)=>{calls.push(['access',id,attachmentId]);return{url:'synthetic://media/'+attachmentId}}}}
 const props=vue.reactive(extra)
 const deps=spec=>{
  if(spec==='vue')return{...vue,onUnmounted:fn=>cleanups.push(fn)}
  if(spec==='@dcloudio/uni-app')return{onLoad(){}}
  if(spec.endsWith('.vue'))return{default:{}}
  if(spec.endsWith('/services'))return services
  if(spec.endsWith('/stores'))return{useRecordStore:()=>store,useTimeChapterStore:()=>({})}
  if(spec.endsWith('/types'))return{RecordStatus,RecordReminderStatus:{},ReplyType:{SHORT_REPLY:'SHORT_REPLY'}}
  if(spec.endsWith('/utils'))return{getToken:()=>state.auth?'synthetic-token':'',hasAuthenticatedSession:()=>state.auth,formatDateTime:x=>String(x||''),toUserMessage:x=>x.message||String(x)}
  if(spec.includes('preview-session'))return{hasPreviewSession:()=>state.preview,showPreviewReadonlyToast:()=>toasts.push('preview readonly')}
  if(spec.includes('useWechatNavMetrics'))return{useWechatNavMetrics:()=>({cssVars:vue.ref({}),navBarHeight:vue.ref(44),navBarTotalHeight:vue.ref(64),rightSafeWidth:vue.ref(96),statusBarHeight:vue.ref(20)})}
  throw Error('Unexpected '+spec)
 }
 const ast=ts.createSourceFile(file,descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true),names=[]
 for(const statement of ast.statements){
  if(ts.isVariableStatement(statement))for(const d of statement.declarationList.declarations)if(ts.isIdentifier(d.name))names.push(d.name.text)
  if(ts.isImportDeclaration(statement)&&statement.importClause&&!statement.importClause.isTypeOnly){const c=statement.importClause;if(c.name)names.push(c.name.text);if(c.namedBindings&&ts.isNamedImports(c.namedBindings))for(const e of c.namedBindings.elements)if(!e.isTypeOnly)names.push(e.name.text)}
 }
 class FakeDate extends Date { constructor(...args){super(...(args.length?args:[clock.now]))}static now(){return clock.now} }
 const context={exports:{},require:deps,uni,getCurrentPages:()=>[],Date:FakeDate,setInterval:fn=>{const id=timers.size+1;timers.set(id,fn);return id},clearInterval:id=>timers.delete(id),setTimeout:()=>0,defineProps:()=>props,withDefaults:(p,defaults)=>{for(const[k,v]of Object.entries(defaults))if(p[k]===undefined)p[k]=typeof v==='function'?v():v;return p}}
 const js=ts.transpileModule(descriptor.scriptSetup.content+'\nglobalThis.surface={'+names.join(',')+'}',{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText
 vm.runInNewContext(js,context,{filename:file});const scope=context.surface
 const render=new Function('Vue',compile(descriptor.template.content,{mode:'function',isCustomElement:()=>true}).code)(vue),target=vue.proxyRefs({...scope})
 const ctx=new Proxy(target,{has:(_,k)=>k!=='_Vue'&&k!=='_cache',get:(o,k)=>k===Symbol.unscopables?undefined:k in o?o[k]:props[k]})
 const nodes=()=>{const all=[];function walk(n){if(!n||typeof n!=='object')return;all.push(n);if(Array.isArray(n.children))n.children.forEach(walk)}walk(render(ctx,[]));return all}
 const select=cls=>nodes().filter(n=>vue.normalizeClass(n.props?.class).split(' ').includes(cls))
 if(scope.currentRecordId){scope.currentRecordId.value=30;scope.applyMemoryPolicy(store.detail)}
 return{scope,props,store,state,clock,timers,cleanups,calls,toasts,modals,nodes,select,setUpdate:fn=>{update=fn}}
}
let passed=0
async function check(name,fn){await fn();passed++;console.log('PASS '+name)}
async function main(){
 await check('封存倒计时沿用真实目标、归零不自动改权限、销毁清定时器',()=>{
  const h=setup('pages/record-detail/index.vue');h.scope.startCountdown(h.store.detail.unlockAt)
  assert.equal([h.scope.countdownH.value,h.scope.countdownM.value,h.scope.countdownS.value].join(':'),'01:02:03')
  h.clock.now+=3723000;h.timers.values().next().value();assert.equal(h.scope.countdownH.value,'00');assert.equal(h.scope.isSealed.value,true)
  assert.equal(h.scope.canEditLaterReflection.value,false);h.cleanups.forEach(fn=>fn());assert.equal(h.timers.size,0);assert.equal(h.calls.length,0)
 })
 await check('封存部分正文复用霜层，默认简洁开关、更多保留备注/篇章',()=>{
  const h=setup('pages/record-detail/index.vue');assert.equal(h.select('sealed-card').length,1);assert.equal(h.select('sealed-expect').length,0);assert.equal(h.select('sealed-sub-hint').length,0);assert.equal(h.select('ownership-delete').length,0)
  assert.equal(h.select('sealed-body-excerpt')[0].children,h.scope.sealedExcerpt.value)
  assert.equal(h.select('memory-policy').length,0);assert.equal(h.select('chapter-membership-panel').length,0)
  const locationName='synthetic long location '.repeat(4);h.store.detail.location={name:locationName,address:'synthetic complete address'}
  assert.equal(h.select('sealed-meta__location')[0].props['aria-label'],locationName.trim());assert.equal(h.select('sealed-meta__address').length,0)
  h.select('sealed-options-toggle')[0].props.onTap();assert.equal(h.select('chapter-membership-panel').length,1);assert.equal(h.select('memory-policy-note').length,1);assert.equal(h.select('sealed-location-details').length,1);assert.equal(h.store.detail.location.name,locationName)
 })
 await check('正文只读截取72个字符且不拆emoji，空响应不补正文或发请求',()=>{
  const h=setup('pages/record-detail/index.vue'),content='synthetic-🙂'.repeat(20)+'synthetic-tail-never-show'
  h.store.detail.content=content
  const expected=Array.from(content).slice(0,72).join('')+'…'
  assert.equal(h.scope.sealedExcerpt.value,expected)
  assert.equal(h.select('sealed-body-excerpt')[0].children,expected)
  assert.equal(h.select('sealed-body-excerpt')[0].props.selectable,false)
  assert.ok(!h.nodes().some(n=>typeof n.children==='string'&&n.children.includes('synthetic-tail-never-show')))
  assert.equal(h.store.detail.content,content);assert.equal(h.scope.isSealed.value,true)
  h.store.detail.content=null;assert.equal(h.scope.sealedExcerpt.value,'');assert.equal(h.select('sealed-body-excerpt').length,0)
  assert.equal(h.select('sealed-body-ink').length,1);assert.equal(h.calls.length,0)
 })
 await check('简洁开关保存真实字段、忙碌防重、失败回滚且不清空原备注',async()=>{
  const h=setup('pages/record-detail/index.vue');let release;h.setUpdate(()=>new Promise(r=>{release=r}))
  const pending=h.scope.toggleSealedMemoryPolicy();await h.scope.toggleSealedMemoryPolicy();assert.equal(h.calls.length,1)
  assert.equal(JSON.stringify(h.calls[0]),JSON.stringify(['memory',30,true,'synthetic existing note']))
  release({...h.store.detail,agentMemoryExcluded:true});await pending;assert.equal(h.scope.memoryExcluded.value,true)
  h.setUpdate(async()=>{throw Error('synthetic failure')});await h.scope.toggleSealedMemoryPolicy();assert.equal(h.scope.memoryExcluded.value,true);assert.equal(h.scope.memoryNoteDraft.value,'synthetic existing note');assert.equal(h.scope.savingMemoryPolicy.value,false)
 })
 await check('Preview拒绝开关写入；删除仍先走确认且取消零删除',async()=>{
  const h=setup('pages/record-detail/index.vue');h.state.auth=false;h.state.preview=true;await h.scope.toggleSealedMemoryPolicy();assert.equal(h.calls.length,0);assert.equal(h.scope.memoryExcluded.value,false)
  h.state.auth=true;h.store.detail.status='SAVED';assert.equal(h.select('ownership-delete').length,1);h.select('ownership-delete')[0].props.onTap();assert.equal(h.modals.length,1);await h.modals[0].success({confirm:false});assert.equal(h.calls.length,0)
 })
 await check('只读媒体过滤非AVAILABLE、图片签名失败重试与封存无媒体空态',async()=>{
  const h=setup('pages/record-detail/components/ReadOnlyRecordMedia.vue',{recordId:30,variant:'sealed',attachments:[{id:1,type:'IMAGE',status:'AVAILABLE'},{id:2,type:'VOICE',status:'AVAILABLE',durationSeconds:7},{id:3,type:'IMAGE',status:'FAILED'}]})
  await vue.nextTick();assert.equal(h.scope.availableAttachments.value.length,2);assert.equal(h.select('readonly-image-item').length,1);assert.equal(h.select('readonly-voice-wave').length,1)
  h.scope.markImageFailed(1);assert.equal(h.scope.imageAccessUrls[1],undefined);await h.scope.previewImage(h.props.attachments[0]);assert.equal(h.scope.imageAccessUrls[1],'synthetic://media/1')
  h.props.attachments=[];assert.equal(h.select('readonly-media').length,0);h.cleanups.forEach(fn=>fn())
 })
 console.log('SEALED DETAIL '+passed+'/'+passed+' PASS; synthetic API/timer only; real provider/network/storage=0')
}
main().catch(error=>{console.error(error);process.exitCode=1})
