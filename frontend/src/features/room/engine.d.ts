import type {Furniture} from './model'
type State={items:Furniture[];editing:boolean;selected:string|null;time:'day'|'evening';gridVisible:boolean}
type Callbacks={select:(id:string)=>void;move:(id:string,x:number,y:number)=>void;open:(id:string)=>void;error:(message:string)=>void;previews:(urls:Record<string,string>)=>void}
export function createRoomRenderer(element:HTMLElement,getState:()=>State,callbacks:Callbacks):{update:()=>Promise<void>;dispose:()=>void}
