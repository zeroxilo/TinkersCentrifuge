package slimegirl.centrifuge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiFluidTank extends FluidTankAnimated {
   private final @NotNull List<FluidStack> fluids;
   public int capacity;

   public MultiFluidTank(int capacity, MantleBlockEntity parent) {
      super(capacity, parent);
      this.fluids = new ArrayList<>();
      this.capacity = capacity;
   }

   public MultiFluidTank(int capacity, MantleBlockEntity parent, List<FluidStack> fluids) {
      super(capacity, parent);
      this.fluids = fluids.stream().map(FluidStack::copy).collect(Collectors.toList());
      this.capacity = capacity;
   }

   public int getSize() {
      return this.fluids.size();
   }

   public @NotNull List<FluidStack> getFluids() {
      return fluids;
   }

   public void setFluid2(int index, FluidStack stack) {
      fluids.set(index, stack);
   }

   public void addFluid(FluidStack stack) {
      fluids.add(stack);
   }

   public FluidStack getFluid(int index) {
      return fluids.get(index);
   }

   public void clearFluids() {
      fluids.clear();
   }

   //读取自nbt
   @Override
   public FluidTank readFromNBT(CompoundTag nbt) {
      clearFluids();
      if (nbt.contains("tanks", Tag.TAG_LIST)) {
         ListTag tankList = nbt.getList("tanks", Tag.TAG_COMPOUND);
         for (int i = 0; i < tankList.size(); i++) {
            CompoundTag tankTag = tankList.getCompound(i);
            addFluid(FluidStack.loadFluidStackFromNBT(tankTag));
         }
      }
      sort();
      return this;
   }

   //写入nbt
   @Override
   public CompoundTag writeToNBT(CompoundTag nbt) {
      ListTag tankList = new ListTag();
      for (final FluidStack fluid : this.fluids) {
         CompoundTag fluidNBT = new CompoundTag();
         fluid.writeToNBT(fluidNBT);
         tankList.add(fluidNBT);
      }
      nbt.put("tanks", tankList);
      return nbt;
   }

   //获取容器数量
   @Override
   public int getTanks() {
      return this.fluids.isEmpty() ? 1 : this.fluids.size();
   }

   //获取容器内流体
   @NotNull
   @Override
   public FluidStack getFluidInTank(int index) {
      if (index < 0 || index >= this.fluids.size()) {
         return FluidStack.EMPTY;
      }
      return this.getFluid(index);
   }
   
   //获取容器容量
   @Override
   public int getTankCapacity(int index) {
      // 如果完全没有流体，唯一的空槽位容量就是机器的最大容量
      if (this.fluids.isEmpty()) {
         return this.capacity;
      }
      if (index < 0 || index >= this.getSize()) {
         return 0;
      }
      // 当前流体的数量 + 容器剩余的所有空闲空间
      return this.getFluid(index).getAmount() + this.getSpace();
   }

   //获取容量
   public int getCapacity() {return this.capacity;}

   //修改整体容量
   public MultiFluidTank setCapacity(int capacity) {
      this.capacity = capacity;
      return this;
   }

   //获取容器内首个可用流体
   @NotNull
   @Override
   public FluidStack getFluid() {
      for (FluidStack fluidStack : this.fluids) {
         if (fluidStack != null && !fluidStack.isEmpty()) {
            return fluidStack;
         }
      }
      return FluidStack.EMPTY;
   }

   //设置流体（讲道理，正常人为什么会让这个储罐用这个方法？）
   public void setFluid(FluidStack stack) {
      clearFluids();
      if (stack != null && !stack.isEmpty()) {
         addFluid(stack.copy());
      }
      onContentsChanged();
   }

   //寻找流体可用的容器的Index，找不到时返回-1
   //每个容器只能分别存储不同类型的流体
   public int findTankIndex(Fluid fluid){
      for (int i = 0; i < getSize(); i++) {
         if (!this.getFluid(i).isEmpty() && this.getFluid(i).getFluid() == fluid) { //相同类型
            return i;
         }
      }
      //如果找不到容器，返回-1
      return -1;
   }

   @Override
   public int fill(FluidStack resource, FluidAction action) {
      return fill2(resource, action);
   }

   //尝试填入流体
   public int fill2(@NotNull FluidStack fluidStack, FluidAction action) {
      if (fluidStack.isEmpty()) {
         return 0;
      }
      int index = findTankIndex(fluidStack.getFluid());
      if (index != -1) {
         return this.fillTo(index, fluidStack, action);
      }

      // No existing tank for this fluid. Create a new one.
      int filled = Math.min(getSpace(), fluidStack.getAmount());
      if (action.execute() && filled > 0) {
         addFluid(new FluidStack(fluidStack, filled));
         onContentsChanged();
      }
      return filled;
   }

   //尝试填入流体至特定槽位
   public int fillTo(int index, FluidStack inputFluidStack, FluidAction action) {
      int filled = 0;
      //获取填充量
      FluidStack toFillFluidStack = this.getFluid(index);
      // why empty??? why not equal?
      if (toFillFluidStack.isEmpty() || toFillFluidStack.isFluidEqual(inputFluidStack)) {
         filled = Math.min(getSpace(), inputFluidStack.getAmount());
      }
      //若指令为执行，尝试填充
      if (filled > 0 && action.execute()) {
         if (toFillFluidStack.isEmpty()) {
            setFluid2(index, new FluidStack(inputFluidStack, filled));
         } else {
            this.getFluid(index).grow(filled);
         }
         this.onContentsChanged();
      }
      return filled;
   }

   //提取特定流体
   @NotNull
   @Override
   public FluidStack drain(FluidStack resource, FluidAction action) {
      int index = findTankIndex(resource.getFluid());
      if(index == -1) return FluidStack.EMPTY; //什么也没有提取
      return this.drainFrom(index,resource.getAmount(), action);
   }
   
   //按流量提取
   @NotNull
   @Override
   public FluidStack drain(int maxAmount, FluidAction action) {
      for (int i = 0; i < getSize(); i++) {
         if (!this.getFluid(i).isEmpty()) {
            return this.drainFrom(i,maxAmount, action);
         }
      }
      return FluidStack.EMPTY;
   }

   //尝试从特定槽位提取流体
   @NotNull
   public FluidStack drainFrom(int index,int maxAmount, FluidAction action) {
      if (this.getFluid(index).isEmpty() || maxAmount == 0) return FluidStack.EMPTY;
      FluidStack fluidHere = this.getFluid(index);
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
         if (fluidHere.isEmpty()) {
            this.sort();
         }
         this.onContentsChanged();
      }
      return (drained > 0)? new FluidStack(fluidtype,drained) : FluidStack.EMPTY;
   }

   //检查给定流体是否可加入任意位置，不检查容量
   public boolean isFluidValid(@NotNull FluidStack fluidStack) {
      return true;
   }

   //检查给定流体是否可加入指定位置，不检查容量
   public boolean isFluidValid(int index, @NotNull FluidStack fluidStack) {
      //每个容器只能存储不同类型的流体
      Fluid fluid = fluidStack.getFluid();
      for (int i = 0; i < getSize(); i++) {
         FluidStack fluidHere = this.getFluid(i);
         if(!fluidHere.isEmpty() && fluidHere.getFluid() == fluid){
            // if we have the fluid, it must be in the current tank
            return i == index;
         }
      }
      //如果没有这种类型，那就可以
      return true;
   }

   //流体排序，让流体全部往前稍稍，挤占空位
   public void sort(){
      this.fluids.removeIf(FluidStack::isEmpty);
   }

   //设置某一容器的流体
   public void setFluid(int index,FluidStack stack) {
      setFluid2(index, stack.copy());
      onContentsChanged();
   }

   //获取是否为全空
   public boolean isEmpty() {
      for(FluidStack fluid : this.fluids){
         if(!fluid.isEmpty()) return false;
      }
      return true;
   }
   
   //获取合计流体量
   public int getFluidAmount() {
      return sum2(fluids);
   }

   @Override
   public int getSpace() {
      return Math.max(0, capacity - getFluidAmount());
   }

   public boolean fitAll(List<FluidStack> outputs) {
      return sum(outputs) <= getSpace();
   }

   public int sum(List<FluidStack> fluidStackList) {
      return fluidStackList.stream().mapToInt(FluidStack::getAmount).sum();
   }

   public int sum2(List<FluidStack> fluidStackList) {
      int amount = 0;
      for (FluidStack fluid : fluidStackList) {
         if(!fluid.isEmpty()){
            amount += fluid.getAmount();
         }
      }
      return amount;
   }
}
