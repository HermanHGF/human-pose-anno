
### 使用说明
1. 拷贝源码, 把它放到一个新建的`jave project`里面
2. 运行(或者`ctrl + F11`), 会弹出一个框
3. 将要标注的`imageset`扔到这个框里面去
4. 剩下的就是开始标注图像了
5. 需要注意的是, 在标完第一张的时候, **稍微等一两秒**, 因为要生成对应的保存目录
6. **源码只是在windows下测试过rgb图像和深度图**

----------

### functions 介绍

- 这里同时标注`human`和`human pose`. 先标注`human`后标注`human pose`
- `human`用一个`bouding box`(两个点: 左上角和右下角)来表示, `human pose`也就是`body/part joints`, 这里假设`body joints`有`15`个
- 在标注`part`的`anno info`时, 先标注`part`的状态 **(这里的状态只有一个: 就是该`part`是否`occluded`, 通常认为该`part`的`area`存在`n%`不可见, 则认为该`part`为`occluded`. 这里个人假设为`n=30`)**
- 然后才标注`part`的`body joint`, 也就是一个点(小圆圈)
- 为了方便标注，其中`non-occ`是默认的
- 鼠标左键：确认点(`joint`)
- 鼠标右键：取消点(`joint`)
- 下面是一些辅助的键盘相应操作
    1. **o**: occluded or not, // 0(default): non-occ, 1: occ
    2. **u**: back to pre-part // just ignore the current part - means resetting
    3. **n**: ahead to next-part   // just ignore the current part - means using the default values
    4. **r**: reset the annotation info of the current image
    5. **s**: save the annotation info of the current image
    6. **a**: back to pre-image    // will delete the label file of the current image if saved
    7. **d**: ahead to next image  // won't save the annotation info of the current image

----------

### 保存格式
- 代码会自动生成标注信息的保存目录,如标注数据集的路径为`/path/to/.../imageset`, 则生成的保存目录为`/path/to/.../imageset_AnnoPose`
- 代码为每个标注图像生成一个标注文件(`.label`), 如`/path/to/.../imageset_AnnoPose/xxx.label`
- 每个`label`的保存内容为: 
  - 第一行为标注图像的路径
  - 从第二行开始, 为这样的格式: `partName/jointName x-coor y-coor occState`, 其中`occState`的值取`0`或`1`
  - 需要注意的是, 第二行和第三行的occState没有意义, 纯粹为了格式一致
>/path/to/.../imageset/xxx.jpg
human_bb_top_left_cornor 57.0 107.0 0
human_bb_bottom_right_cornor 208.0 279.0 0
head 105.0 150.0 0
neck 114.0 158.0 0
torso 98.0 163.0 0
left_shoulder 79.0 153.0 0
left_elbow 73.0 152.0 0
left_hand/wrist 120.0 154.0 0
right_shoulder 112.0 138.0 0
right_elbow 89.0 143.0 0
right_hand/wrist 142.0 190.0 0
left_hip 170.0 215.0 0
left_knee 164.0 257.0 0
left_foot/ankle 164.0 186.0 0
right_hip 189.0 210.0 0
right_knee 181.0 251.0 0
right_foot/ankle 136.0 170.0 0

----------

### **example**

~~暂时不能从本地上传图片, 所有给不了标注时的效果图~~

----------