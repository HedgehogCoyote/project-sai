import type {Furniture} from './model'
export type RoomSouvenir={id:string;kind:string;title:string;photoUrl:string|null}
type State={items:Furniture[];editing:boolean;selected:string|null;time:'day'|'evening';gridVisible:boolean;souvenirs?:RoomSouvenir[];colors?:{primary:string;secondary:string}|null;badges?:Record<string,number>}
type Callbacks={select:(id:string)=>void;move:(id:string,x:number,y:number)=>void;open:(id:string)=>void;error:(message:string)=>void;previews:(urls:Record<string,string>)=>void}
export function createRoomRenderer(element:HTMLElement,getState:()=>State,callbacks:Callbacks):{update:()=>Promise<void>;dispose:()=>void}
