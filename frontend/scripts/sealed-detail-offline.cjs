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
 return{scope,props,store,state,clock,timers,cleanups,calls,toasts,modals,nodes,select,setUpdate:fn=>{update=fn},setService:(name,value)=>Object.assign(services[name]||(services[name]={}),value)}
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
 await check('回看默认保留阅读/后来其实/轻对话/回应，辅助功能收更多',()=>{
  const h=setup('pages/record-detail/index.vue');Object.assign(h.store.detail,{status:'UNLOCKED',canReply:true,realityLaterSubmitCount:0,location:{name:'synthetic place',address:'synthetic address'}})
  assert.equal(h.select('unlock-hero').length,1);assert.equal(h.select('unlock-card-text')[0].children,h.store.detail.content)
  assert.equal(h.select('reflection-textarea').length,1);assert.equal(h.select('review-entry').length,1);assert.equal(h.select('unlock-cta').length,1)
  assert.equal(h.select('chapter-membership-panel').length,0);assert.equal(h.select('memory-policy').length,0);assert.equal(h.select('ownership-delete').length,0)
  h.select('review-more-toggle')[0].props.onTap();assert.equal(h.select('chapter-membership-panel').length,1);assert.equal(h.select('memory-policy').length,0);assert.equal(h.select('memory-note-entry').length,1);assert.equal(h.select('ownership-delete').length,1);assert.equal(h.select('unlock-location').length,1)
 })
 await check('参考小浮层预填原值，取消不写入且其他状态不显示入口',()=>{
  const h=setup('pages/record-detail/index.vue');h.store.detail.status='UNLOCKED'
  assert.equal(h.select('memory-note-entry').length,1);assert.equal(h.select('memory-note-panel').length,0)
  h.select('memory-note-entry')[0].props.onTap();assert.equal(h.select('memory-note-panel').length,1)
  assert.equal(h.scope.memorySheetNote.value,'synthetic existing note');assert.equal(h.scope.memorySheetExcluded.value,false)
  h.scope.memorySheetNote.value='synthetic unsaved note';h.select('memory-note-toggle')[0].props.onTap()
  h.select('memory-note-mask')[0].props.onTap();assert.equal(h.scope.showMemoryNoteSheet.value,false)
  assert.equal(h.scope.memoryNoteDraft.value,'synthetic existing note');assert.equal(h.scope.memoryExcluded.value,false);assert.equal(h.calls.length,0)
  h.scope.openMemoryNoteSheet();assert.equal(h.scope.memorySheetNote.value,'synthetic existing note');h.select('memory-note-close')[0].props.onTap()
  for(const status of ['SEALED','SAVED','DRAFT']){h.store.detail.status=status;h.scope.openMemoryNoteSheet();assert.equal(h.select('memory-note-entry').length,0);assert.equal(h.select('memory-note-panel').length,0)}
 })
 await check('备注保存失败保留输入可重试，忙碌防重且响应成功才关闭',async()=>{
  const h=setup('pages/record-detail/index.vue');h.store.detail.status='UNLOCKED';h.scope.openMemoryNoteSheet()
  h.scope.memorySheetNote.value='  synthetic new note  ';h.scope.memorySheetExcluded.value=true
  h.setUpdate(async()=>{throw Error('synthetic failure')});await h.scope.saveMemoryNoteSheet()
  assert.equal(h.scope.showMemoryNoteSheet.value,true);assert.equal(h.scope.memorySheetNote.value,'  synthetic new note  ')
  assert.equal(h.store.detail.agentMemoryContextNote,'synthetic existing note');assert.equal(h.scope.memoryExcluded.value,false)
  let release;h.setUpdate(()=>new Promise(r=>{release=r}));const count=h.calls.length,pending=h.select('memory-note-save')[0].props.onTap()
  await h.scope.saveMemoryNoteSheet();h.scope.closeMemoryNoteSheet();assert.equal(h.calls.length,count+1);assert.equal(h.scope.showMemoryNoteSheet.value,true)
  assert.equal(h.select('memory-note-input')[0].props.disabled,true)
  assert.equal(JSON.stringify(h.calls.at(-1)),JSON.stringify(['memory',30,true,'synthetic new note']))
  release({...h.store.detail,agentMemoryExcluded:true,agentMemoryContextNote:'synthetic confirmed note'});await pending
  assert.equal(h.scope.showMemoryNoteSheet.value,false);assert.equal(h.scope.memoryNoteDraft.value,'synthetic confirmed note');assert.equal(h.scope.memoryExcluded.value,true)
  h.scope.openMemoryNoteSheet();assert.equal(h.scope.memorySheetNote.value,'synthetic confirmed note')
  h.scope.memorySheetNote.value='   ';h.setUpdate(async(id,excluded,note)=>({...h.store.detail,agentMemoryExcluded:excluded,agentMemoryContextNote:note}));await h.scope.saveMemoryNoteSheet()
  assert.equal(h.calls.at(-1)[3],null)
 })
 await check('备注Preview保存拒绝且保留草稿，零请求与已确认设置不变',async()=>{
  const h=setup('pages/record-detail/index.vue');h.store.detail.status='UNLOCKED';h.state.auth=false;h.state.preview=true
  h.scope.openMemoryNoteSheet();h.scope.memorySheetNote.value='synthetic preview note';h.scope.memorySheetExcluded.value=true;await h.scope.saveMemoryNoteSheet()
  assert.equal(h.calls.length,0);assert.equal(h.scope.showMemoryNoteSheet.value,true);assert.equal(h.scope.memorySheetNote.value,'synthetic preview note')
  assert.equal(h.scope.memoryExcluded.value,false);assert.equal(h.scope.memoryNoteDraft.value,'synthetic existing note');assert.equal(h.toasts.at(-1),'preview readonly')
 })
 await check('后来其实失败保留输入、响应后更新、两次后只读',async()=>{
  const h=setup('pages/record-detail/index.vue');Object.assign(h.store.detail,{status:'UNLOCKED',realityLaterSubmitCount:0});h.scope.laterReflectionDraft.value='synthetic reflection'
  h.setService('recordService',{updateLaterReflection:async(id,text)=>{h.calls.push(['reflection',id,text]);throw Error('synthetic failure')}})
  await h.scope.submitLaterReflection();assert.equal(h.scope.laterReflectionDraft.value,'synthetic reflection');assert.equal(h.scope.submittingLaterReflection.value,false)
  let release;h.setService('recordService',{updateLaterReflection:async(id,text)=>{h.calls.push(['reflection',id,text]);return new Promise(resolve=>{release=resolve})}})
  const countBefore=h.calls.length,pending=h.select('reflection-save')[0].props.onTap();h.select('reflection-save')[0].props.onTap();assert.equal(h.calls.length,countBefore+1)
  release({...h.store.detail,realityLater:'synthetic reflection',realityLaterSubmitCount:1});await pending;assert.equal(h.store.detail.realityLater,'synthetic reflection');assert.equal(h.scope.laterReflectionCount.value,1)
  h.store.detail.realityLaterSubmitCount=2;assert.equal(h.select('reflection-textarea').length,0);assert.equal(h.select('reflection-save').length,0)
  const count=h.calls.length;await h.scope.submitLaterReflection();assert.equal(h.calls.length,count)
 })
 await check('回应浮层互斥、失败保输入、已回应不重复提交、读回失败可重试',async()=>{
  const h=setup('pages/record-detail/index.vue');Object.assign(h.store.detail,{status:'UNLOCKED',canReply:true,hasReply:false});h.scope.showReviewChat.value=true
  h.select('unlock-cta')[0].props.onTap();assert.equal(h.scope.showReplySheet.value,true);assert.equal(h.scope.showReviewChat.value,false)
  h.scope.replyContent.value='synthetic response'
  h.setService('replyService',{submitReply:async()=>{throw Error('synthetic failure')}})
  await h.scope.submitReply();assert.equal(h.scope.replyContent.value,'synthetic response');assert.equal(h.scope.showReplySheet.value,true)
  h.setService('replyService',{submitReply:async()=>{h.calls.push(['reply']);h.store.detail.hasReply=true},getReply:async()=>{throw Error('synthetic read failure')}})
  await h.scope.submitReply();assert.equal(h.scope.replyContent.value,'');assert.equal(h.scope.replyLoadFailed.value,true);assert.equal(h.select('unlock-replied-retry').length,1);assert.equal(h.select('unlock-cta').length,0)
  h.setService('replyService',{getReply:async()=>({content:'synthetic saved response'})});h.select('unlock-replied-retry')[0].props.onTap();for(let tick=0;tick<8&&h.scope.replyLoading.value;tick++)await Promise.resolve()
  assert.equal(h.scope.replyLoading.value,false);assert.equal(h.scope.replyLoadFailed.value,false);assert.equal(h.select('unlock-replied-text')[0].children,'synthetic saved response')
  const count=h.calls.length;h.scope.replyContent.value='blocked duplicate';await h.scope.submitReply();assert.equal(h.calls.length,count)
 })
 await check('回看Preview仍拒绝回应/后来其实与真实对话调用',async()=>{
  const h=setup('pages/record-detail/index.vue');Object.assign(h.store.detail,{status:'UNLOCKED',canReply:true});h.state.auth=false;h.state.preview=true
  h.scope.replyContent.value='synthetic';h.scope.laterReflectionDraft.value='synthetic'
  await h.scope.submitReply();await h.scope.submitLaterReflection();await h.scope.openReviewChat()
  assert.equal(h.calls.length,0);assert.equal(h.scope.showReviewChat.value,false);assert.ok(h.toasts.every(x=>x==='preview readonly'))
 })
 await check('回看媒体用独立变体、真实时长与封面且不改SAVED变体',async()=>{
  const h=setup('pages/record-detail/components/ReadOnlyRecordMedia.vue',{recordId:30,variant:'review',coverId:1,attachments:[{id:1,type:'IMAGE',status:'AVAILABLE'},{id:2,type:'IMAGE',status:'AVAILABLE'},{id:3,type:'VOICE',status:'AVAILABLE',durationSeconds:18}]})
  await vue.nextTick();assert.equal(h.select('readonly-image-item').length,2);assert.equal(h.select('readonly-cover-label').length,1);assert.equal(h.select('readonly-voice-caption')[0].children,'00:18 原始声音')
  h.props.variant='unlocked';assert.equal(h.select('readonly-voice-caption').length,0);assert.equal(h.select('readonly-voice-name').length,1);h.cleanups.forEach(fn=>fn())
 })
 console.log('DETAIL '+passed+'/'+passed+' PASS; synthetic API/timer only; real provider/network/storage=0')
}
main().catch(error=>{console.error(error);process.exitCode=1})
