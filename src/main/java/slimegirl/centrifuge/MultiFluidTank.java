package slimegirl.centrifuge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;

import org.jetbrains.annotations.NotNull;

public class MultiFluidTank extends FluidTankAnimated{
   public @NotNull List<FluidStack> fluids;
   public int capacity;

   public MultiFluidTank(int size,int capacity, MantleBlockEntity parent) {
      super(capacity, parent);
      this.fluids = new ArrayList<>();
      this.capacity = capacity;
      for(int i = 0;i < size;i++){
         this.fluids.add(FluidStack.EMPTY);
      }
   }

   //读取自nbt
   @Override
   public FluidTank readFromNBT(CompoundTag nbt) {
      for(int i = 0;i < this.fluids.size();i++){
         CompoundTag subNbt = nbt.getCompound("tank"+i);
         if(subNbt != null){
            this.fluids.set(i,FluidStack.loadFluidStackFromNBT(subNbt));
         }
      }
      return this;
   }

   //写入nbt
   @Override
   public CompoundTag writeToNBT(CompoundTag nbt) {
      for(int i = 0;i < this.fluids.size();i++){
         CompoundTag subNbt = new CompoundTag();
         if(!this.fluids.get(i).isEmpty()){
            this.fluids.get(i).writeToNBT(subNbt);
            nbt.put("tank"+i, subNbt);
         }
      }
      return nbt;
   }

   //获取容器数量
   @Override
   public int getTanks() {
      return this.fluids.size();
   }

   //获取容器内流体
   @Override
   public FluidStack getFluidInTank(int index) {
      return this.fluids.get(index);
   }
   
   //获取容器容量
   @Override
   public int getTankCapacity(int arg0) {return this.capacity;}

   //获取整体容量
   public int getCapacity() {return this.capacity * this.fluids.size();}

   
   //修改整体容量
   public MultiFluidTank setCapacity(int capacity) {
      setCapacity(capacity,true);
      return this;
   }

   public MultiFluidTank setCapacity(int capacity,boolean whetherEachTank) {
      if(whetherEachTank){
         this.capacity = capacity;
      }else{
         int eachCapacity = capacity / this.fluids.size();
         this.capacity = eachCapacity;
      }
      return this;
   }

   //获取容器内首个可用流体
   @Override
   public FluidStack getFluid() {
      for(int i = 0;i< this.fluids.size();i++){
         if (!this.fluids.isEmpty()){
            return this.fluids.get(i);
         }
      }
      return FluidStack.EMPTY;
   }

   //寻找流体可用的容器的Index，找不到时返回-1
   //每个容器只能分别存储不同类型的流体
   public int findTankIndex(Fluid fluid){
      int index = -1;
      int firstEmpty = -1;
      for(int i = 0;i< this.fluids.size();i++){
         if(this.fluids.get(i).isEmpty()){ //为空，进行记录
            if(firstEmpty == -1) firstEmpty = i;
         }else if(this.fluids.get(i).getFluid() == fluid){ //相同类型
            index = i;
            break;
         }
      }
      //如果容器内没有这种材料，且存在空容器，用空容器处理它。
      if(index == -1 && firstEmpty != -1) index = firstEmpty;
      //如果找不到容器，返回-1
      return index;
   }

   //检查是否有充足空间
   public boolean fit(FluidStack resource){
      int index = findTankIndex(resource.getFluid());
      if(index == -1) return false; //没有地方填充
      int filled = this.fillTo(index,resource,FluidAction.SIMULATE);
      return filled == resource.getAmount();
   }
   
   //检查是否全部有充足空间
   //用于反合金配方
   public boolean fitAll(List<FluidStack> resources){
      boolean noSpace = false;
      int useEmpties = 0;
      for(FluidStack resource : resources){
         Fluid resFluid = resource.getFluid();
         int resAmount = resource.getAmount();
         boolean foundSame = false;
         for(int i = 0;i < this.fluids.size();i++){
            if(!this.fluids.get(i).isEmpty() && this.fluids.get(i).getFluid() == resFluid){
               if(this.getSpace(i) < resAmount){
                  return false; //同类别的储罐里不够它存
               }
               foundSame = true;
               break;
            }
         }
         if(!foundSame) useEmpties++; //没找到同类，则采用空储罐
         if(noSpace) return false;
      }
      //检查是否有充足的空储罐
      if(useEmpties > 0){
         for(FluidStack fluid : this.fluids){
            if(fluid.isEmpty()){
               useEmpties -= 1;
            }
         }
         if(useEmpties > 0) return false; //空储罐不够多
      }
      return true;
   }

   //尝试填入流体
   @Override
   public int fill(FluidStack resource, FluidAction action) {
      int index = findTankIndex(resource.getFluid());
      if(index == -1) return 0; //填充了0mb
      return this.fillTo(index,resource,action);
   }

   //尝试填入流体至特定槽位
   public int fillTo(int index,FluidStack resource, FluidAction action){
      int filled = 0;
      if(!resource.isEmpty()){
         //获取填充量
         FluidStack fluidHere = this.fluids.get(index);
         if (fluidHere.isEmpty()) {
            filled = Math.min(this.capacity, resource.getAmount());
         } else if(fluidHere.isFluidEqual(resource)){
            filled = Math.min(this.capacity - fluidHere.getAmount(), resource.getAmount());
         }
         //若指令为执行，尝试填充
         if(filled > 0 && action.execute()){
            if (fluidHere.isEmpty()) {
               this.fluids.set(index,new FluidStack(resource, filled));
            }else{
               this.fluids.get(index).grow(filled);
            }
            this.onContentsChanged();
         }
      }
      return filled;
   }

   //提取特定流体
   @Override
   public FluidStack drain(FluidStack resource, FluidAction action) {
      int index = findTankIndex(resource.getFluid());
      if(index == -1) return FluidStack.EMPTY; //什么也没有提取
      if(this.fluids.get(index).isEmpty()) return FluidStack.EMPTY; //什么也没有提取
      return this.drainFrom(index,resource.getAmount(), action);
   }

   //按流量提取
   @Override
   public FluidStack drain(int maxAmount, FluidAction action) {
      for(int i = 0;i< this.fluids.size();i++){
         if(!this.fluids.get(i).isEmpty()){
            return this.drainFrom(i,maxAmount, action);
         }
      }
      return FluidStack.EMPTY;
   }
   
   //尝试从特定槽位提取流体
   public FluidStack drainFrom(int index,int maxAmount, FluidAction action) {
      if(this.fluids.get(index).isEmpty() || maxAmount == 0) return FluidStack.EMPTY;
      FluidStack fluidHere = this.fluids.get(index);
      Fluid fluidtype = fluidHere.getFluid(); //存一个防止被整理时出现问题
      //获取汲取量
      int drained = 0;
      if (fluidHere.getAmount() < maxAmount) {
         drained = fluidHere.getAmount();
      }else{
         drained = maxAmount;
      }
      //若指令为执行，尝试填充
      if (action.execute() && drained > 0) {
         fluidHere.shrink(drained);
         if(fluidHere.isEmpty() && fluidHere.getAmount() == 0){
            this.sort();
         }
         this.onContentsChanged();
      }
      return (drained > 0)? new FluidStack(fluidtype,drained) : FluidStack.EMPTY;
   }

   //检查给定流体是否可加入任意位置，不检查容量
   public boolean isFluidValid(@NotNull FluidStack fluidStack) {
      //每个容器只能存储不同类型的流体
      Fluid fluid = fluidStack.getFluid();
      boolean hasEmpty = false;
      for(int i = 0;i< this.fluids.size();i++){
         FluidStack fluidHere = this.fluids.get(i);
         if(fluidHere.isEmpty()){
            hasEmpty = true;
         }else if(fluidHere.getFluid() == fluid){
            return true; //相同类型
         }
      }
      //如果没有这种类型，返回是否有空位
      return hasEmpty;
   }

   //检查给定流体是否可加入指定位置，不检查容量
   public boolean isFluidValid(int index, @NotNull FluidStack fluidStack) {
      //每个容器只能存储不同类型的流体
      Fluid fluid = fluidStack.getFluid();
      for(int i = 0;i< this.fluids.size();i++){
         FluidStack fluidHere = this.fluids.get(i);
         if(!fluidHere.isEmpty() && fluidHere.getFluid() == fluid){
            if(i == index){
               return true; //相同类型
            }
         }
      }
      //如果没有这种类型，看看对应位置是否为空
      return this.fluids.get(index).isEmpty();
   }

   //流体排序，让流体全部往前稍稍，挤占空位
   public void sort(){
      for(int i = 0;i < this.fluids.size();i++){
         if(this.fluids.get(i).isEmpty()){
            //若为空，将后续容器的内容填充到此处
            for(int j = i;j < this.fluids.size();j++){
               if(!this.fluids.get(j).isEmpty()){
                  this.fluids.set(i,this.fluids.get(j));
                  this.fluids.set(j,FluidStack.EMPTY);
                  break;
               }
            }
         }
      }
   }

   //设置流体（讲道理，正常人为什么会让这个储罐用这个方法？）
   public void setFluid(FluidStack stack) {
      this.fluids.set(0,stack);
      //清空其余容器
      for(int i = 1;i < this.fluids.size();i++){
         this.fluids.set(i,FluidStack.EMPTY);
      }
   }

   //设置某一容器的流体
   public void setFluid(int index,FluidStack stack) {
      this.fluids.set(index,stack);
   }

   //获取是否为全空
   public boolean isEmpty() {
      for(FluidStack fluid : this.fluids){
         if(!fluid.isEmpty()) return false;
      }
      return true;
   }

   //获取某一容器是否为空
   public boolean isEmpty(int index) {
      return this.fluids.get(index).isEmpty();
   }

   //获取合计剩余空间
   public int getSpace() {
      int space = 0;
      for(FluidStack fluid : this.fluids){
         if(!fluid.isEmpty()){
            space += Math.max(0, this.capacity - fluid.getAmount());
         }else{
            space += this.capacity;
         }
      }
      return space;
   }
   
   //获取合计流体量
   public int getFluidAmount() {
      int amount = 0;
      for(FluidStack fluid : this.fluids){
         if(!fluid.isEmpty()){
            amount += fluid.getAmount();
         }
      }
      return amount;
   }

   //获取某一容器的剩余空间
   public int getSpace(int index) {
      return Math.max(0, this.capacity - this.fluids.get(index).getAmount());
   }
}
