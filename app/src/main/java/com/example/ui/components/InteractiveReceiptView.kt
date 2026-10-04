package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoreConfigEntity
import com.example.ui.viewmodels.DualReceiptRow
import java.text.DecimalFormat

val ReceiptInkNavy = Color(0xFF1E3A8A)
val ReceiptInkBlueAccent = Color(0xFF1D4ED8)
val ReceiptInkRed = Color(0xFFDC2626)
val ReceiptPaperWhite = Color(0xFFFFFFFF)

@Composable
fun InteractiveReceiptView(
    storeConfig: StoreConfigEntity,
    invoiceNumber: Int,
    onInvoiceNumberChange: (Int) -> Unit,
    dateString: String,
    onDateStringChange: (String) -> Unit,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    paymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    dualRows: List<DualReceiptRow>,
    onUpdateRightDescription: (index: Int, desc: String) -> Unit,
    onUpdateRightQuantity: (index: Int, qty: String) -> Unit,
    onUpdateRightTotalAmount: (index: Int, total: String) -> Unit,
    onUpdateLeftDescription: (index: Int, desc: String) -> Unit,
    onUpdateLeftQuantity: (index: Int, qty: String) -> Unit,
    onUpdateLeftTotalAmount: (index: Int, total: String) -> Unit,
    onRemoveRow: (index: Int) -> Unit,
    onAddManualRow: () -> Unit,
    isEditable: Boolean = true,
    modifier: Modifier = Modifier
) {
    val formatter = DecimalFormat("#,##0.##")

    val rightSubtotal = dualRows.sumOf { it.rightTotal }
    val leftSubtotal = dualRows.sumOf { it.leftTotal }
    val grandTotal = rightSubtotal + leftSubtotal

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ReceiptPaperWhite)
            .border(1.5.dp, ReceiptInkNavy, RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. ULTRA-COMPACT STORE HEADER (Minimal single-strip bar to save maximum screen space)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ReceiptInkNavy.copy(alpha = 0.05f))
                    .border(1.dp, ReceiptInkNavy, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Store Name & Phone (بقالة العزي مع رقم الجوال)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = storeConfig.storeNameArabic.ifEmpty { "بقالة العزي" },
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = ReceiptInkBlueAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "جوال: ${storeConfig.phone1.ifEmpty { "772437314" }}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }

                // Payment Type Cash/Credit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(0.8.dp, ReceiptInkNavy, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.clickable(enabled = isEditable) { onPaymentTypeChange("نقداً") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (paymentType == "نقداً") "[✓] نقداً" else "[ ] نقداً",
                            fontSize = 8.5.sp,
                            color = if (paymentType == "نقداً") ReceiptInkBlueAccent else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        modifier = Modifier.clickable(enabled = isEditable) { onPaymentTypeChange("أجل") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (paymentType == "أجل") "[✓] أجل" else "[ ] أجل",
                            fontSize = 8.5.sp,
                            color = if (paymentType == "أجل") ReceiptInkRed else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Invoice Number & Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "رقم: ", fontSize = 8.5.sp, color = ReceiptInkNavy, fontWeight = FontWeight.Bold)
                    if (isEditable) {
                        BasicTextField(
                            value = invoiceNumber.toString(),
                            onValueChange = { onInvoiceNumberChange(it.toIntOrNull() ?: invoiceNumber) },
                            textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black, color = ReceiptInkRed),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(45.dp)
                        )
                    } else {
                        Text(text = "$invoiceNumber", fontSize = 11.sp, color = ReceiptInkRed, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(text = "التاريخ: ", fontSize = 8.5.sp, color = ReceiptInkNavy, fontWeight = FontWeight.Bold)
                    if (isEditable) {
                        BasicTextField(
                            value = dateString,
                            onValueChange = onDateStringChange,
                            textStyle = TextStyle(fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                            singleLine = true,
                            modifier = Modifier.width(62.dp)
                        )
                    } else {
                        Text(text = dateString, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 2. CUSTOMER NAME LINE ("المطلوب من الأخ")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.8.dp, ReceiptInkNavy, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المطلوب من الأخ: ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReceiptInkNavy
                )
                if (isEditable) {
                    BasicTextField(
                        value = customerName,
                        onValueChange = onCustomerNameChange,
                        textStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                        singleLine = true,
                        cursorBrush = SolidColor(ReceiptInkNavy),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = customerName.ifEmpty { "عميل نقدي" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 3. TABLE HEADER: ORDER FROM RIGHT TO LEFT
            // Right Section: [ القيمة الإجمالية | العدد | التفاصيل ]
            // Divider
            // Left Section:  [ القيمة الإجمالية | العدد | التفاصيل ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ReceiptInkNavy)
                    .padding(vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RIGHT SECTION (RTL: Total -> Qty -> Desc)
                HeaderCell(text = "القيمة الإجمالية", weight = 0.75f)
                HeaderCell(text = "العدد", weight = 0.35f)
                HeaderCell(text = "التفاصيل (البيان)", weight = 1.4f)

                // Vertical Divider between dual sections
                Box(modifier = Modifier.width(1.5.dp).fillMaxHeight().background(Color.White))

                // LEFT SECTION (RTL: Total -> Qty -> Desc)
                HeaderCell(text = "القيمة الإجمالية", weight = 0.75f)
                HeaderCell(text = "العدد", weight = 0.35f)
                HeaderCell(text = "التفاصيل (البيان)", weight = 1.4f)
            }

            // 4. DYNAMIC SMART ROWS (Starts with 1 row, expands automatically on typing in the last row)
            dualRows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            val y = size.height - strokeWidth / 2
                            drawLine(
                                color = ReceiptInkNavy.copy(alpha = 0.25f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth
                            )
                        }
                        .padding(vertical = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // RIGHT SECTION CELLS
                    DataCell(
                        value = row.rightTotalAmountStr,
                        onValueChange = { onUpdateRightTotalAmount(index, it) },
                        isEditable = isEditable,
                        weight = 0.75f,
                        isNumeric = true,
                        textColor = ReceiptInkNavy,
                        fontWeight = FontWeight.Bold,
                        placeholder = if (isEditable && index == dualRows.lastIndex && row.isCompletelyEmpty) "0" else ""
                    )
                    DataCell(
                        value = row.rightQuantityStr,
                        onValueChange = { onUpdateRightQuantity(index, it) },
                        isEditable = isEditable,
                        weight = 0.35f,
                        isNumeric = true
                    )
                    DataCell(
                        value = row.rightDescription,
                        onValueChange = { onUpdateRightDescription(index, it) },
                        isEditable = isEditable,
                        weight = 1.4f,
                        align = TextAlign.Start,
                        placeholder = if (isEditable && index == dualRows.lastIndex && row.isCompletelyEmpty) "اكتب الصنف..." else ""
                    )

                    // Vertical Divider
                    Box(modifier = Modifier.width(1.5.dp).height(20.dp).background(ReceiptInkNavy.copy(alpha = 0.4f)))

                    // LEFT SECTION CELLS
                    DataCell(
                        value = row.leftTotalAmountStr,
                        onValueChange = { onUpdateLeftTotalAmount(index, it) },
                        isEditable = isEditable,
                        weight = 0.75f,
                        isNumeric = true,
                        textColor = ReceiptInkNavy,
                        fontWeight = FontWeight.Bold
                    )
                    DataCell(
                        value = row.leftQuantityStr,
                        onValueChange = { onUpdateLeftQuantity(index, it) },
                        isEditable = isEditable,
                        weight = 0.35f,
                        isNumeric = true
                    )
                    DataCell(
                        value = row.leftDescription,
                        onValueChange = { onUpdateLeftDescription(index, it) },
                        isEditable = isEditable,
                        weight = 1.4f,
                        align = TextAlign.Start
                    )
                }
            }

            // 5. SECTION SUBTOTALS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ReceiptInkNavy.copy(alpha = 0.08f))
                    .padding(vertical = 2.5.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الإجمالي الأيمن: ${formatter.format(rightSubtotal)}",
                    modifier = Modifier.weight(2.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReceiptInkBlueAccent,
                    textAlign = TextAlign.Center
                )

                Box(modifier = Modifier.width(1.dp).height(12.dp).background(ReceiptInkNavy))

                Text(
                    text = "الإجمالي الأيسر: ${formatter.format(leftSubtotal)}",
                    modifier = Modifier.weight(2.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReceiptInkBlueAccent,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 6. GRAND TOTAL SUMMARY BOX (المبلغ الإجمالي الكلي)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, ReceiptInkNavy, RoundedCornerShape(3.dp))
                    .background(ReceiptInkNavy.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المبلغ الإجمالي الكلي (Grand Total):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = ReceiptInkNavy
                )
                Text(
                    text = "${formatter.format(grandTotal)} ${storeConfig.currencySymbol}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = ReceiptInkBlueAccent
                )
            }

            if (isEditable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = onAddManualRow,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+ إضافة سطر يدوي", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 7. COMPACT FOOTER
            Text(
                text = storeConfig.defaultDisclaimerNote,
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "توقيع المشتري: ............", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = ReceiptInkNavy)
                Text(text = "توقيع البائع: ............", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = ReceiptInkNavy)
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = Color.White,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RowScope.DataCell(
    value: String,
    onValueChange: (String) -> Unit,
    isEditable: Boolean,
    weight: Float,
    isNumeric: Boolean = false,
    align: TextAlign = TextAlign.Center,
    textColor: Color = Color.Black,
    fontWeight: FontWeight = FontWeight.Normal,
    placeholder: String = ""
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 1.dp),
        contentAlignment = when (align) {
            TextAlign.Start -> Alignment.CenterStart
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        if (isEditable) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = fontWeight,
                    color = textColor,
                    textAlign = align
                ),
                keyboardOptions = if (isNumeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                singleLine = true,
                cursorBrush = SolidColor(ReceiptInkNavy),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 8.5.sp,
                            color = Color.Gray.copy(alpha = 0.6f),
                            textAlign = align
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = value,
                fontSize = 9.sp,
                fontWeight = fontWeight,
                color = textColor,
                textAlign = align,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
